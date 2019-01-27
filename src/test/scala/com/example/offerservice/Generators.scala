package com.example.offerservice

import org.scalacheck.Gen
import Models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Arbitrary

object Generators {
    sealed trait Request
    sealed trait Query extends Request
    case class GetOne(id: String) extends Query
    case object GetAll extends Query
    sealed trait Mutation extends Request
    case class Deletion(id: String) extends Mutation
    case class Addition(offer: Offer, idToGenerate: String) extends Mutation

    val nonEmptyString = Gen.alphaStr.suchThat(!_.isEmpty)
    val offerGen: Gen[Offer] = for {
        name <- nonEmptyString
        desc <- nonEmptyString
        price <- arbitrary[BigDecimal]
    } yield Offer(name, desc, price)

    val nonNegativePricedOfferGen: Gen[Offer] = for {
        name <- nonEmptyString
        desc <- nonEmptyString
        price <- arbitrary[BigDecimal].suchThat(_ >= 0)
    } yield Offer(name, desc, price)

    val negativePricedOfferGen: Gen[Offer] = for {
        name <- nonEmptyString
        desc <- nonEmptyString
        price <- arbitrary[BigDecimal].suchThat(_ < 0)
    } yield Offer(name, desc, price)

    implicit val arbOffer: Arbitrary[Offer] = Arbitrary(offerGen)

    def chooseId(ids: Seq[String]) = Gen.frequency((1, nonEmptyString), (2, Gen.oneOf(ids)))

    def additionGen(ids: Seq[String]) = for {
        offer <- offerGen
        idToGenerate <- Gen.oneOf(ids)
    } yield Addition(offer, idToGenerate)

    def requestWithIdGen[T <: Request](ids: Seq[String], mkRequest: String => T): Gen[Request] = for {
        id <- chooseId(ids)
    } yield mkRequest(id)

    val noDeletionsRequestsGen: Gen[Seq[Request]] = for {
        ids <- Gen.nonEmptyListOf(nonEmptyString)
        additions <- Gen.listOf(additionGen(ids))
        getOnes <- Gen.listOf(requestWithIdGen(ids, GetOne(_)))
        getAlls <- Gen.listOf(GetAll)
        requests <- {
            val l = additions ++ getOnes ++ getAlls
            Gen.pick(l.length, l)
        }
    } yield requests

    val requestsGen: Gen[Seq[Request]] = for {
        ids <- Gen.nonEmptyListOf(nonEmptyString)
        additions <- Gen.listOf(additionGen(ids))
        deletions <- Gen.listOf(requestWithIdGen(ids, Deletion(_)))
        getOnes <- Gen.listOf(requestWithIdGen(ids, GetOne(_)))
        getAlls <- Gen.listOf(GetAll)
        requests <- {
            val l = additions ++ deletions ++ getOnes ++ getAlls
            Gen.pick(l.length, l)
        }
    } yield requests
}