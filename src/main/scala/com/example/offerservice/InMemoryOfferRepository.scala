package com.example.offerservice

import collection.mutable.Map
import cats.effect.IO
import Models._
import java.util.UUID

final class InMemoryOfferRepository(
    os: collection.immutable.Map[String, Offer],
) extends OfferRepository {
    private val offers = Map(os.toSeq: _*)
    private val generateId = IO { 
        UUID.randomUUID().toString
    }
    def addOffer(offer: Offer) = for {
        id <- generateId
        _ <- IO {
            offers += id -> offer
        }
    } yield id

    def getOffer(id: String) = IO {
        for {
            offer <- offers.get(id)
        } yield RecordWithId(offer, id)
    }
    def getAllOffers: IO[Seq[OfferWithId]] = IO {
        offers.toList.map(p => new RecordWithId(p._2, p._1))
    }
    def deleteOffer(id: String): IO[Either[String, Unit]] = IO {
        offers.remove(id)
    }.map({
        case None => Left("Cannot delete an offer that doesn't exist")
        case Some(_) => Right()
    })
}