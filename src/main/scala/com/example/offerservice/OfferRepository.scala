package com.example.offerservice

import cats.effect.IO
import Models._

trait OfferRepository {
    def getOffer(id: String): IO[Option[OfferWithId]]
    def getAllOffers: IO[Seq[OfferWithId]]
    def addOffer(offer: Offer): IO[String]
    def deleteOffer(id: String): IO[Either[String, Unit]]
}