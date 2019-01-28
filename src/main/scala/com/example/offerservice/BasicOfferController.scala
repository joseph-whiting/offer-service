package com.example.offerservice

import Models._

import cats.effect._
import scala.concurrent.duration.MILLISECONDS

final class BasicOfferController(
    private val repo: OfferRepository
)(
    private implicit val clock: Clock[IO],
) extends OfferController {
    def addOffer(offer: Offer) = {
        if (offer.price >= 0) {
            Right(clock.monotonic(MILLISECONDS).flatMap(repo.addOffer(offer, _)))
        } else {
            Left("Cannot create an offer with negative price")
        }
    }

    def cancelOffer(id: String) = repo.deleteOffer(id)
    def getOffer(id: String): IO[Option[StoredOffer]] = repo.getOffer(id)
    def getAllOffers: IO[Seq[StoredOffer]] = repo.getAllOffers
}