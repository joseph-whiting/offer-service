package com.example.offerservice

object Models {
    case class Offer(name: String, description: String, price: BigDecimal)
    case class RecordWithId[TRecord](record: TRecord, id: String)
    type OfferWithId = RecordWithId[Offer]
}