package com.example.offerservice

import cats.effect.IO
import Models._

trait OfferRepository {
    def getOffer(id: String): IO[Option[StoredOffer]]
    def getAllOffers: IO[Seq[StoredOffer]]
    def addOffer(offer: Offer, creationTime: Long): IO[String]
    def deleteOffer(id: String): IO[Either[String, Unit]]
}