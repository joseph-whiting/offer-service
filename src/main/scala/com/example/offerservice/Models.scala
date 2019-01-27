package com.example.offerservice

object Models {
    case class Offer(name: String, description: String, price: BigDecimal)
    case class OfferWithId(record: Offer, id: String)
}