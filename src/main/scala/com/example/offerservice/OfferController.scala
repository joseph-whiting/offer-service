package com.example.offerservice

import Models._

import cats.effect.IO

trait OfferController {
    def addOffer(offer: Offer): Either[String, IO[String]]
    def cancelOffer(id: String): IO[Either[String, Unit]]
    def getOffer(id: String): IO[Option[StoredOffer]]
    def getAllOffers: IO[Seq[StoredOffer]]
}