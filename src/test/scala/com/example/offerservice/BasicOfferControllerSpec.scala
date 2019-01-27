package com.example.offerservice

import cats.effect.IO
import org.scalatest._
import org.scalamock.scalatest.MockFactory
import Models._

class BasicOfferControlllerSpec extends FunSpec with MockFactory {
    describe("cancel offer") {
        it("should delete the offer from the repo") {
            val repo = mock[OfferRepository]
            (repo.deleteOffer _).expects("id").returns(IO {
                Left("something")
            })
            val controller = new BasicOfferController(repo)
            val result = controller.cancelOffer("id").unsafeRunSync
            assert(result == Left("something"))
        }
    }

    describe("add offer") {
        it("should add the offer to the repo if the price is non-negative") {
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", 0)
            (repo.addOffer _).expects(offer).returns(IO {
                "id"
            })
            val controller = new BasicOfferController(repo)
            val result = controller.addOffer(offer).map(_.unsafeRunSync)
            assert(result == Right("id"))
        }

        it("should return an error and not add to the repo if the price is negative") {
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", -1)
            (repo.addOffer _).expects(*).never
            val controller = new BasicOfferController(repo)
            val result = controller.addOffer(offer).map(_.unsafeRunSync)
            assert(result == Left("Cannot create an offer with negative price"))
        }
    }

    describe("get offer") {
        it("should get the offer from the repo") {
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", 0)
            (repo.getOffer _).expects("id").returns(IO {
                Some(new RecordWithId(offer, "id"))
            })
            val controller = new BasicOfferController(repo)
            val result = controller.getOffer("id").unsafeRunSync
            assert(result == Some(new RecordWithId(offer, "id")))
        }
    }

    describe("get all offers") {
        it("should getAll offers from the repo") {
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", 0)
            (repo.getAllOffers _).expects.returns(IO {
                List(new RecordWithId(offer, "id"))
            })
            val controller = new BasicOfferController(repo)
            val result = controller.getAllOffers.unsafeRunSync
            assert(result == List(new RecordWithId(offer, "id")))
        }
    }
}