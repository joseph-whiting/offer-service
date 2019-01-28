package com.example.offerservice

import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder
import cats.implicits._
import cats.effect._

import scala.concurrent.ExecutionContext

object HelloWorldServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream
}

object ServerStream {

  def offerService = new OfferService(
    new BasicOfferController(new InMemoryOfferRepository())(Clock.create)
  ).service

  def stream(implicit ec: ExecutionContext) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(offerService, "/")
      .serve
}
