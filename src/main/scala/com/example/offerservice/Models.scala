package com.example.offerservice

object Models {
    case class Offer(
        name: String,
        description: String,
        price: BigDecimal,
        secondsToExpiry: Long,
    )
    case class StoredOffer(record: Offer, id: String, creationTime: Long)
}