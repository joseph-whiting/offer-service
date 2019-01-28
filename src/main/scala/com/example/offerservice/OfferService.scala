package com.example.offerservice

import cats.effect.IO
import org.http4s._, org.http4s.dsl.io._, org.http4s.implicits._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import cats.effect._

import Models._

class OfferService(controller: OfferController) {
  implicit val offerEncoder: Encoder[Offer] = deriveEncoder[Offer]
  implicit val recordWithIdEncoder: Encoder[StoredOffer] = deriveEncoder[StoredOffer]
  implicit val offerDecoder = jsonOf[IO, Offer]
  
  val service = HttpService[IO] {
      case GET -> Root / "offers" => controller.getAllOffers
        .flatMap(offers => Response(status = Status.Ok).withBody(offers.asJson))
      case req @ POST -> Root / "offers" => for {
        offer <- req.as[Offer]
        resp <- controller.addOffer(offer).fold(
            (e: String) => BadRequest(e),
            (io: IO[String]) => for {
              id <- io
              r <- Ok(id)
            } yield r
        )
      } yield resp
      case GET -> Root / "offers" / id => for {
        offer <- controller.getOffer(id)
        resp <- offer.fold(NotFound())(o => Ok(o.asJson))
      } yield resp
      case DELETE -> Root / "offers" / id => for {
        result <- controller.cancelOffer(id)
        resp <- result.fold(NotFound(_), _ => Ok())
      } yield resp
  }
}
