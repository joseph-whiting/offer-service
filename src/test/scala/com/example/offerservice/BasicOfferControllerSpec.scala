package com.example.offerservice

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

import cats.effect.IO

import Models._
import Generators._

class StubOfferRepo extends OfferRepository {
    var added = Set[Offer]()
    var deleted = Set[String]()
    var got = Set[String]()
    var gotAll = false

    def getOffer(id: String): IO[Option[OfferWithId]] = IO {
        got = got + id
        Some(RecordWithId(Offer("a name", "a description", 10), id))
    }
    def getAllOffers: IO[Seq[OfferWithId]] = ???
    def addOffer(offer: Offer): IO[String] = IO {
        added = added + offer
        "someId"
    }
    def deleteOffer(id: String): IO[Either[String, Unit]] = IO {
        deleted = deleted + id
        Right()
    }
}

object BasicOfferControlllerSpec extends Properties("BasicOfferControlllerSpec") {
    property("add offer should add all non-negative priced offers") = forAll(nonNegativePricedOfferGen) { offer => 
        val repo = new StubOfferRepo()
        val controller = new BasicOfferController(repo)

        val result = controller.addOffer(offer) match {
            case Left(e) => throw new Error("failed")
            case Right(io) => io.unsafeRunSync()
        }
        result == "someId" &&
        repo.added == Set(offer)

    }

    property("add offer should return error for negative priced offers") = forAll(negativePricedOfferGen) { offer => 
        val repo = new StubOfferRepo()
        val controller = new BasicOfferController(repo)
        repo.added.size == 0 &&
            controller.addOffer(offer) == Left("Cannot create an offer with negative price")
    }

    property("cancel offer should delete from repo") = forAll(nonEmptyString) { id => 
        val repo = new StubOfferRepo()
        val controller = new BasicOfferController(repo)
        val result = controller.cancelOffer(id).unsafeRunSync()
        result.isRight && repo.deleted == Set(id)
    }

    property("get offer should get from repo") = forAll(nonEmptyString) { id => 
        val repo = new StubOfferRepo()
        val controller = new BasicOfferController(repo)
        val result = controller.getOffer(id).unsafeRunSync()
        result == Some(RecordWithId(Offer("a name", "a description", 10), id)) && repo.got == Set(id)
    }

    // property("get all offers should get from repo") = forAll(nonEmptyString) { id => 
    //     val repo = new StubOfferRepo()
    //     val controller = new BasicOfferController(repo)
    //     val result = controller.getAllOffers.unsafeRunSync()
    //     result == List(RecordWithId(Offer("a name", "a description", 10), id), RecordWithId(Offer("another name", "another description", 20), id)) &&
    //         repo.gotAll
    // }
}