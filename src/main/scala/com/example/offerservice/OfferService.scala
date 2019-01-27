package com.example.offerservice

import cats.effect.Effect
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._

class HelloWorldService[F[_]: Effect] extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "offers" =>
        Ok(Json.obj("offers" -> Json.fromString("offers")))
    }
  }
}
