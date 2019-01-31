package com.example.offerservice

import cats.effect.{IO, IOApp, ExitCode, ContextShift, Timer}
import cats.implicits._

import org.http4s.server.blaze._
import org.http4s.server.Router
import org.http4s._, org.http4s.dsl.io._

import fs2.Stream
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.util.concurrent.ScheduledExecutorService

import cats._
import cats.data.{Kleisli, OptionT}

object HelloWorldServer extends IOApp {
  import scala.concurrent.ExecutionContext.Implicits.global

  def run(args: List[String]): IO[ExitCode] = stream.compile.drain.as(ExitCode.Success)

  def controller = new BasicOfferController(
    new InMemoryOfferRepository(),
    1 second
  )

  def offerService = new OfferService(controller).service

  def stream: Stream[IO, ExitCode] = {
    val httpRoute: HttpRoutes[IO] = Router("/" -> offerService)

    // temporary workaround for some strange implicit behaviour
    val httpApp: HttpApp[IO] = Kleisli((a: Request[IO]) => httpRoute.run(a).getOrElse(Response.notFound))
    Stream.eval(controller.maintainRepositoryForever.start(contextShift)).drain ++ BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(httpApp)
      .serve
  }
}