package com.example.offerservice

import Models._

import org.scalatest._
import cats._
import cats.data._
import cats.implicits._
import cats.effect.IO
import scala.util.matching.Regex

class InMemoryOfferRepositorySpec extends FunSpec {
    describe("InMemoryOfferRepository") {
        describe("get") {
            it("should return None if the id doesn't exist") {
                val repo = new InMemoryOfferRepository()
                assert(repo.getOffer("id2").unsafeRunSync == None)
            }

            it("should return Some(StoredOffer) if the id is that of an offer that has been added") {
                val repo = new InMemoryOfferRepository()
                val offer = Offer("name", "desc", 10, 0)
                val io = for {
                    id <- repo.addOffer(offer, 0)
                    o <- repo.getOffer(id)
                } yield o
                assert(io.unsafeRunSync.map(_.record) == Some(offer))
            }

            it("should return None if the id is that of an offer that has been added, and then deleted") {
                val repo = new InMemoryOfferRepository()
                val offer = Offer("name", "desc", 10, 0)
                val io = for {
                    id <- repo.addOffer(offer, 0)
                    _ <- repo.deleteOffer(id)
                    o <- repo.getOffer(id)
                } yield o
                assert(io.unsafeRunSync.map(_.record) == None)
            }
        }

        describe("getAll") {
            it("should return all offers that have been added and not deleted") {
                val offer1 = Offer("name1", "desc1", 1, 0)
                val offer2 = Offer("name2", "desc2", 2, 0)
                val offer3 = Offer("name3", "desc3", 3, 0)
                val repo = new InMemoryOfferRepository()
                val io = for {
                    _ <- repo.addOffer(offer1, 0)
                    id3 <- repo.addOffer(offer3, 0)
                    _ <- repo.addOffer(offer2, 0)
                    _ <- repo.deleteOffer(id3)
                    offers <- repo.getAllOffers
                } yield offers
                assert(io.unsafeRunSync.map(_.record).toSet == Set(offer1, offer2))
            }
        }

        describe("delete") {
            it("should return an error message if the id doesn't exist") {
                val repo = new InMemoryOfferRepository()
                assert(repo.deleteOffer("some id").unsafeRunSync == Left("Cannot delete an offer that doesn't exist"))
            }

            it("should return Right() if the id exists") {
                val repo = new InMemoryOfferRepository()
                val io = for {
                    id <- repo.addOffer(new Offer("name", "desc", 10, 0), 0)
                    x <- repo.deleteOffer(id)
                } yield x
                assert(io.unsafeRunSync == Right())
            }
        }

        describe("add") {
            it("should generate url-safe ids") {
                val repo = new InMemoryOfferRepository()
                val offer = Offer("name", "desc", 10, 0)
                val id = repo.addOffer(offer, 0).unsafeRunSync
                val urlSafeRegex = new Regex("^[a-zA-Z0-9_-]*$")
                assert(!urlSafeRegex.findFirstIn(id).isEmpty)
            }
        }
    }
}