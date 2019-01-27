package com.example.offerservice

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

import Models._
import Generators._

import scala.collection.mutable.Map
import scala.collection.mutable.Set

import cats._
import cats.data._
import cats.implicits._
import cats.effect.IO

import java.util.UUID

object InMemoryOfferRepositorySpec extends Properties("InMemoryOfferRepository") {
    sealed trait Response
    case class Added(id: String, request: Addition) extends Response
    case class Deleted(request: Deletion) extends Response
    case class DeleteFailed(request: Deletion) extends Response
    case class OfferGot(offer: Option[OfferWithId], request: GetOne) extends Response
    case class AllOffersGot(offers: Seq[OfferWithId]) extends Response

    def runRequests(requests: Seq[Request]): IO[List[Response]] = {
        var ids = requests.collect({
            case Addition(offer: Offer, idToGenerate: String) => idToGenerate
        })
        val generateId = IO {
            val id = ids.head
            ids = ids.tail
            id
        }
        val repo = new InMemoryOfferRepository(Map.empty, generateId)
        requests.map({
            case Addition(offer, id) => repo.addOffer(offer).map(Added(_, Addition(offer, id)))
            case Deletion(id) => repo.deleteOffer(id).map({
                case Left(_) => DeleteFailed(Deletion(id))
                case Right(_) => Deleted(Deletion(id))
            })
            case GetOne(id) => repo.getOffer(id).map(OfferGot(_, GetOne(id)))
            case GetAll => repo.getAllOffers.map(AllOffersGot(_))
        }).toList.sequence
    }

    property("without deletions, getAll returns all offers that were previously added") = forAll(noDeletionsRequestsGen) { (requests: Seq[Request]) => 
        val responses = runRequests(requests).unsafeRunSync
        @scala.annotation.tailrec
        def check(l: List[Response]): Boolean = l match {
            case (h: AllOffersGot) :: t => t.collect({case Added(id, _) => id}).toSet == h.offers.map(_.id).toSet && check(t)
            case h :: t => check(t)
            case Nil => true
        }
        check(responses.reverse)
    }

    property("without deletions, get returns the offer if and only if it was previously added") = forAll(noDeletionsRequestsGen) { (requests: Seq[Request]) => 
        val responses = runRequests(requests).unsafeRunSync
        println(responses)
        def check(l: List[Response]): Boolean = l match {
            case OfferGot(offer, r) :: t => t.collectFirst({ case Added(id, Addition(addedOffer, _)) if id == r.id => addedOffer }) == offer && check(t)
            case h :: t => check(t)
            case Nil => true
        }
        check(responses.reverse)
    }
}