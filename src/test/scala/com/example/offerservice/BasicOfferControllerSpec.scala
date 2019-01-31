package com.example.offerservice

import cats.effect.{IO, Clock, Fiber}
import org.scalatest._
import org.scalamock.scalatest.MockFactory
import Models._
import scala.concurrent.duration.TimeUnit
import cats.effect.laws.util.TestContext
import scala.concurrent.duration._
import cats.data._
import cats.implicits._


class BasicOfferControlllerSpec extends FunSpec with MockFactory {
    describe("cancel offer") {
        it("should delete the offer from the repo") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]
            (repo.deleteOffer _).expects("id").returns(IO {
                Left("something")
            })
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer)
            val result = controller.cancelOffer("id").unsafeRunSync
            assert(result == Left("something"))
        }
    }

    describe("add offer") {
        it("should add the offer to the repo if the price is non-negative") {
            val testContext = TestContext()
            testContext.tick(42 seconds)
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", 0, 0)
            (repo.addOffer _).expects(offer, 42).returns(IO {
                "id"
            })
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            val result = controller.addOffer(offer).map(_.unsafeRunSync)
            assert(result == Right("id"))
        }

        it("should return an error and not add to the repo if the price is negative") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", -1, 0)
            (repo.addOffer _).expects(*, *).never
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            val result = controller.addOffer(offer).map(_.unsafeRunSync)
            assert(result == Left("Cannot create an offer with negative price"))
        }
    }

    describe("maintainRepository") {
        it("should periodically check for expired offers in the repository") {
            var checks = 0
            val offers: Seq[StoredOffer] = Nil
            val getAll = IO {
                checks = checks + 1
                offers
            }
            val testContext = TestContext()
            val repo = mock[OfferRepository]
            val controller = new BasicOfferController(repo, 30 seconds)(testContext.timer[IO])
            (repo.getAllOffers _).expects.atLeastOnce.returns(getAll)
            val wait = IO {
                testContext.tick(42 seconds)
            }
            val test = for {
                fiber <- controller.maintainRepositoryForever.start(IO.contextShift(testContext))
                _ <- wait
            } yield fiber
            val f = test.unsafeRunSync
            assert(checks == 2)
            f.cancel
        }

        it("should delete the expired offers at any given time") {
            val offers: Seq[StoredOffer] = List(
                StoredOffer(
                    Offer("n1", "d1", 10, 10),
                    "id1",
                    0,
                ), // expires at 10 seconds
                StoredOffer(
                    Offer("n2", "d2", 10, 90),
                    "id2",
                    10,
                ), // expires at 100 seconds
                StoredOffer(
                    Offer("n3", "d3", 10, 10),
                    "id3",
                    90,
                ), // expires at 100 seconds
                StoredOffer(
                    Offer("n4", "d4", 10, 5),
                    "id4",
                    5,
                ), // expires at 10 seconds
            )
            class StubRepo extends OfferRepository {
                val deleted = scala.collection.mutable.Set[String]()
                def deleteOffer(id: String) = IO {
                    deleted.add(id)
                    Right(())
                }
                def getAllOffers = IO.pure(offers)
                def addOffer(offer: Offer, creationTime: Long) = ???
                def getOffer(id: String) = ???
            }

            val getAll = IO.pure(offers)
            val testContext = TestContext()
            val repo = new StubRepo()
            val controller = new BasicOfferController(repo, 1 seconds)(testContext.timer[IO])
            def wait(time: FiniteDuration) = IO {
                testContext.tick(time)
            }
            val test = for {
                fiber <- controller.maintainRepositoryForever.start(IO.contextShift(testContext))
                _ <- wait(10 seconds)
            } yield fiber
            val f = test.unsafeRunSync
            assert(repo.deleted === Set("id1", "id4"))
            f.cancel
        }
    }

    describe("get offer") {
        it("should get the offer from the repo") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", 0, 2)
            (repo.getOffer _).expects("id").returns(IO {
                Some(StoredOffer(offer, "id", 0))
            })
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            val result = controller.getOffer("id").unsafeRunSync
            assert(result == Some(StoredOffer(offer, "id", 0)))
        }

        it("should not return offers that have expired") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]

            // offer lasting 3 created at 10
            val offer = Offer("name", "desc", 0, 3)
            (repo.getOffer _).expects("id").returns(IO {
                Some(StoredOffer(offer, "id", 10))
            })
            testContext.tick(14 seconds)
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            val result = controller.getOffer("id").unsafeRunSync
            val calculated = Offer("name", "desc", 0, 1)
            assert(result == None)
        }

        it("should return an secondsUntilExpiration wrt now") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]

            // offer lasting 3 created at 10
            val offer = Offer("name", "desc", 0, 3)
            (repo.getOffer _).expects("id").returns(IO {
                Some(StoredOffer(offer, "id", 10))
            })
            testContext.tick(12 seconds)
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            val result = controller.getOffer("id").unsafeRunSync
            val calculated = Offer("name", "desc", 0, 1)
            assert(result == Some(StoredOffer(calculated, "id", 10)))
        }
    }

    describe("get all offers") {
        it("should getAll offers from the repo") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]
            val offer = Offer("name", "desc", 0, 10)
            val offers = List.fill(2)(StoredOffer(offer, "id", 0))
            (repo.getAllOffers _).expects.returns(IO {
                offers
            })
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            val result = controller.getAllOffers.unsafeRunSync
            assert(result == offers)
        }

        it("should not return offers that have expired, and should return expiry times wrt now") {
            val testContext = TestContext()
            val repo = mock[OfferRepository]
            val expiredOffers = List.fill(2)(StoredOffer(Offer("name", "expired!", 0, 10), "id", 10))
            val nonExpiredOffers = List.fill(2)(StoredOffer(Offer("name", "not expired!", 0, 15), "id", 10))
            (repo.getAllOffers _).expects.returns(IO {
                expiredOffers ++ nonExpiredOffers
            })
            val controller = new BasicOfferController(repo, 1 second)(testContext.timer[IO])
            testContext.tick(22 seconds)
            val result = controller.getAllOffers.unsafeRunSync
            val expectedOffers = List.fill(2)(StoredOffer(Offer("name", "not expired!", 0, 3), "id", 10))
            assert(result == expectedOffers)
        }
    }
}