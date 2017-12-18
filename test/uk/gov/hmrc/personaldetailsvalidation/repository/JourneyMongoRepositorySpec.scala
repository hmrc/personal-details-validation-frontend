/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.personaldetailsvalidation.repository

import java.time.ZoneOffset.UTC
import java.time.{Duration, LocalDateTime}

import akka.Done
import generators.Generators.Implicits._
import mongo.MongoIndexVerifier
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Descending
import reactivemongo.bson.BSONDocument
import uk.gov.hmrc.datetime.CurrentTimeProvider
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators._
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class JourneyMongoRepositorySpec
  extends UnitSpec
    with MongoSpecSupport
    with MongoIndexVerifier
    with MockFactory
    with ScalaFutures
    with IntegrationPatience {

  "persist" should {

    "persist the given journeyId and relativeUrl tuple so it can be retrieved" in new Setup {
      repository.persist(journeyId -> relativeUrl).futureValue shouldBe Done

      repository.findById(journeyId).futureValue shouldBe Some(journeyId -> relativeUrl)
    }

    "add 'createdAt' field with current time when persisting the document" in new Setup {
      await(repository.persist(journeyId -> relativeUrl))

      val selector = Some(reactivemongo.bson.BSONDocument(
        "_id" -> journeyId.value.toString,
        "createdAt" -> currentTime.atZone(UTC).toInstant.toEpochMilli)
      )
      bsonCollection(repository.collection.name)().count(selector = selector).futureValue shouldBe 1
    }
  }

  "journeyExists" should {

    "return true if there is a journey for the given JourneyId" in new Setup {
      repository.insert(journeyId -> relativeUrl).futureValue
      repository.journeyExists(journeyId).futureValue shouldBe true
    }

    "return false if there is no journey with the given JourneyId" in new Setup {
      repository.journeyExists(journeyId).futureValue shouldBe false
    }
  }

  "repository" should {
    "create ttl on collection" in new Setup {
      val expectedIndex = Index(Seq("createdAt" -> Descending), name = Some("journey-ttl-index"), options = BSONDocument("expireAfterSeconds" -> ttlSeconds))
      verify(expectedIndex).on(repository.collection.name)
    }
  }


  private trait Setup {

    await(mongo().drop())

    val journeyId: JourneyId = journeyIds.generateOne
    val relativeUrl = relativeUrls.generateOne

    implicit val ttlSeconds: Long = 100
    implicit val currentTimeProvider = stub[CurrentTimeProvider]
    val currentTime: LocalDateTime = LocalDateTime.now()
    currentTimeProvider.apply _ when() returns currentTime

    val config = new JourneyMongoRepositoryConfig(mock[Configuration]) {
      override lazy val collectionTtl: Duration = Duration.ofSeconds(ttlSeconds)
    }

    val repository = new JourneyMongoRepository(config, new ReactiveMongoComponent {
      override val mongoConnector: MongoConnector = mongoConnectorForTest
    })
  }

}
