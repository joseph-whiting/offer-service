package com.example.offerservice

import collection.mutable.Map
import cats.effect.IO
import Models._
import java.util.UUID

final class InMemoryOfferRepository extends OfferRepository {
    private val offers = Map[String, StoredOffer]()
    private val generateId = IO { 
        UUID.randomUUID().toString
    }
    def addOffer(offer: Offer, creationTime: Long) = for {
        id <- generateId
        _ <- IO {
            offers += id -> StoredOffer(offer, id, creationTime)
        }
    } yield id

    def getOffer(id: String) = IO {
        for {
            offer <- offers.get(id)
        } yield offer
    }

    def getAllOffers: IO[Seq[StoredOffer]] = IO {
        offers.values.toList
    }

    def deleteOffer(id: String): IO[Either[String, Unit]] = IO {
        offers.remove(id)
    }.map({
        case None => Left("Cannot delete an offer that doesn't exist")
        case Some(_) => Right()
    })
}