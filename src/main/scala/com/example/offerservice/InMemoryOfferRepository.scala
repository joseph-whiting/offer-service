package com.example.offerservice

import collection.mutable.Map
import cats.effect.IO
import Models._
import java.util.UUID

// IO { 
//         UUID.randomUUID().toString
// }
final class InMemoryOfferRepository(
    private val offers: Map[String, Offer],
    private val generateId: IO[String],
) extends OfferRepository {

    def addOffer(offer: Offer) = for {
        id <- generateId
        _ <- IO {
            offers += id -> offer // error?
            println(offers)
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
        offers -= id // TODO - if doesn't exist
        Right()
    }
}