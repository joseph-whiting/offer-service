package com.example.offerservice

import cats.effect.IO
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

import org.scalatest.FunSpec
import org.scalamock.scalatest.MockFactory

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

import Models._

class OfferServiceSpec extends FunSpec with MockFactory {
    describe("OfferService") {
        def check[A](
            actual: IO[Response[IO]], 
            expectedStatus: Status, 
            expectedBody:   Option[A])(
            implicit ev: EntityDecoder[IO, A]
        ): Boolean =  {
            val actualResp         = actual.unsafeRunSync
            val statusCheck        = actualResp.status == expectedStatus 
            val bodyCheck          = expectedBody.fold[Boolean](
                actualResp.body.compile.toVector.unsafeRunSync.isEmpty,
            )(
                expected => actualResp.as[A].unsafeRunSync == expected
            )
            statusCheck && bodyCheck   
        }
        describe("GET /offers") {
            it("should get offers from the controller and return as json, with OK status") {
                val controller = mock[OfferController]
                (controller.getAllOffers _).expects().returns(IO {
                    List(
                        StoredOffer(Offer("name1", "desc1", 1, 0), "id1", 0),
                        StoredOffer(Offer("name2", "desc2", 2, 0), "id2", 0),
                    ),
                })
                val service = (new OfferService(controller)).service
                val getAllOffers = Request[IO](Method.GET, Uri.uri("/offers"))
                val response = service.run(getAllOffers).getOrElseF(NotFound())
                val expectedJson = Json.arr(
                    Json.obj(
                        "record" -> Json.obj(
                            "name" -> Json.fromString("name1"),
                            "description" -> Json.fromString("desc1"),
                            "price" -> Json.fromInt(1),
                            "secondsToExpiry" -> Json.fromInt(0)
                        ),
                        "id" -> Json.fromString("id1"),
                        "creationTime" -> Json.fromInt(0)
                    ),
                    Json.obj(
                        "record" -> Json.obj(
                            "name" -> Json.fromString("name2"),
                            "description" -> Json.fromString("desc2"),
                            "price" -> Json.fromInt(2),
                            "secondsToExpiry" -> Json.fromInt(0)
                        ),
                        "id" -> Json.fromString("id2"),
                        "creationTime" -> Json.fromInt(0)
                    ),
                )
                assert(check[Json](response, Status.Ok, Some(expectedJson)))
            }
        }

        describe("POST /offers") {
            it("should add a new offer with the controller, returning OK & id if successful") {
                val controller = mock[OfferController]
                val offer = Offer("name1", "desc1", 1, 0)
                (controller.addOffer _).expects(offer).once.returns(Right(IO {"anId"}))
                val service = (new OfferService(controller)).service
                val response = for {
                    addOffer <- Request[IO](
                        method = Method.POST,
                        uri = Uri.uri("/offers"),
                    ).withBody(Json.obj(
                        "name" -> Json.fromString("name1"),
                        "description" -> Json.fromString("desc1"),
                        "price" -> Json.fromInt(1),
                        "secondsToExpiry" -> Json.fromInt(0)
                    ))
                    r <- service.run(addOffer).getOrElseF(NotFound())
                } yield r
                assert(check[String](response, Status.Ok, Some("anId")))
            }

            it("should add a new offer with the controller, returning BadRequest & error if successful") {
                val controller = mock[OfferController]
                val offer = Offer("name1", "desc1", 1, 0)
                (controller.addOffer _).expects(offer).returns(Left("an error"))
                val service = (new OfferService(controller)).service
                val response = for {
                    addOffer <- Request[IO](
                        method = Method.POST,
                        uri = Uri.uri("/offers"),
                    ).withBody(Json.obj(
                        "name" -> Json.fromString("name1"),
                        "description" -> Json.fromString("desc1"),
                        "price" -> Json.fromInt(1),
                        "secondsToExpiry" -> Json.fromInt(0)
                    ))
                    r <- service.run(addOffer).getOrElseF(NotFound())
                } yield r
                assert(check[String](response, Status.BadRequest, Some("an error")))
            }
        }

        describe("GET /offers/{id}") {
            it("should get the offer with that id from the controller, returning OK & offer if successful") {
                val controller = mock[OfferController]
                val offer = Offer("name1", "desc1", 1, 0)
                val storedOffer = StoredOffer(offer, "id1", 0)
                (controller.getOffer _).expects("id1").returns(IO {Some(storedOffer)})
                val service = (new OfferService(controller)).service
                val getOffer = Request[IO](
                    method = Method.GET,
                    uri = Uri.uri("/offers/id1"),
                )
                val expectedJson =  Json.obj(
                    "record" -> Json.obj(
                        "name" -> Json.fromString("name1"),
                        "description" -> Json.fromString("desc1"),
                        "price" -> Json.fromInt(1),
                        "secondsToExpiry" -> Json.fromInt(0)
                    ),
                    "id" -> Json.fromString("id1"),
                    "creationTime" -> Json.fromInt(0)
                )
                val response = service.run(getOffer).getOrElseF(NotFound())
                assert(check(response, Status.Ok, Some(expectedJson)))
            }

            it("should return NotFound if no offer returned from controller") {
                val controller = mock[OfferController]
                val offer = Offer("name1", "desc1", 1, 0)
                val storedOffer = StoredOffer(offer, "id1", 0)
                (controller.getOffer _).expects("id1").returns(IO {None})
                val service = (new OfferService(controller)).service
                val getOffer = Request[IO](
                    method = Method.GET,
                    uri = Uri.uri("/offers/id1"),
                )
                val response = service.run(getOffer).getOrElseF(NotFound())
                assert(check(response, Status.NotFound, None: Option[Json]))
            }
        }

        describe("DELETE /offers/{id}") {
            it("should cancel the offer with that id from the controller, returning OK if successful") {
                val controller = mock[OfferController]
                val offer = Offer("name1", "desc1", 1, 0)
                val storedOffer = StoredOffer(offer, "id1", 0)
                (controller.cancelOffer _).expects("id1").returns(IO { Right() })
                val service = (new OfferService(controller)).service
                val cancelOffer = Request[IO](
                    method = Method.DELETE,
                    uri = Uri.uri("/offers/id1"),
                )

                val response = service.run(cancelOffer).getOrElseF(NotFound())
                assert(check(response, Status.Ok, None: Option[Json]))
            }
            it("should cancel the offer with that id from the controller, returning NotFound if error") {
                val controller = mock[OfferController]
                val offer = Offer("name1", "desc1", 1, 0)
                val storedOffer = StoredOffer(offer, "id1", 0)
                (controller.cancelOffer _).expects("id1").returns(IO { Left("I had trouble") })
                val service = (new OfferService(controller)).service
                val cancelOffer = Request[IO](
                    method = Method.DELETE,
                    uri = Uri.uri("/offers/id1"),
                )

                val response = service.run(cancelOffer).getOrElseF(NotFound())
                assert(check(response, Status.NotFound, Some("I had trouble")))
            }
        }
    }
}