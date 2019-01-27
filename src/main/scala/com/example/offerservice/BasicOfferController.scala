package com.example.offerservice

import Models._

import cats.effect.IO

final class BasicOfferController(private val repo: OfferRepository) extends OfferController {
    def addOffer(offer: Offer) = {
        if (offer.price >= 0) {
            Right(repo.addOffer(offer))
        } else {
            Left("Cannot create an offer with negative price")
        }
    }

    def cancelOffer(id: String) = repo.deleteOffer(id)
    def getOffer(id: String): IO[Option[OfferWithId]] = repo.getOffer(id)
    def getAllOffers: IO[Seq[OfferWithId]] = repo.getAllOffers
}