package com.example.offerservice

import Models._
import cats.data._
import cats.implicits._
import cats.effect._
import scala.concurrent.duration._
import cats.implicits._

final class BasicOfferController(
    private val repo: OfferRepository,
    private val flushCycle: FiniteDuration
)(
    private implicit val timer: Timer[IO],
) extends OfferController {
    def addOffer(offer: Offer) = {
        if (offer.price >= 0) {
            Right(timer.clock.monotonic(SECONDS).flatMap(repo.addOffer(offer, _)))
        } else {
            Left("Cannot create an offer with negative price")
        }
    }

    def cancelOffer(id: String) = repo.deleteOffer(id)
    def getOffer(id: String): IO[Option[StoredOffer]] = for {
        result <- repo.getOffer(id)
        now <- timer.clock.monotonic(SECONDS)
    } yield result
        .filter(s => s.record.secondsToExpiry + s.creationTime > now)
        .map(stored => StoredOffer(
            Offer(
                stored.record.name,
                stored.record.description,
                stored.record.price,
                stored.record.secondsToExpiry - (now - stored.creationTime),
            ),
            stored.id,
            stored.creationTime
        ))
    def getAllOffers: IO[Seq[StoredOffer]] = for {
        result <- repo.getAllOffers
        now <- timer.clock.monotonic(SECONDS)
    } yield result
        .filter(s => s.record.secondsToExpiry + s.creationTime > now)
        .map(stored => StoredOffer(
            Offer(
                stored.record.name,
                stored.record.description,
                stored.record.price,
                stored.record.secondsToExpiry - (now - stored.creationTime),
            ),
            stored.id,
            stored.creationTime
        ))
    def maintainRepositoryForever: IO[Unit] = for {
        _ <- flushExpiredOffers
        _ <- timer.sleep(flushCycle)
        _ <- maintainRepositoryForever
    } yield ()


    private def flushExpiredOffers(): IO[Unit] = for {
        allOffers <- repo.getAllOffers
        now <- timer.clock.monotonic(SECONDS)
        _ <- allOffers
            .filter(o => o.creationTime + o.record.secondsToExpiry <= now)
            .map(_.id)
            .map(cancelOffer(_))
            .toList
            .sequence // log if errors?
    } yield ()
}