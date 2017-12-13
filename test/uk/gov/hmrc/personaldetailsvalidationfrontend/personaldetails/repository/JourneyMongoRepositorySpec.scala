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

package uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails.repository

import org.scalatest.concurrent.ScalaFutures
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.personaldetailsvalidationfrontend.generators.Generators.Implicits._
import uk.gov.hmrc.personaldetailsvalidationfrontend.generators.ValuesGenerators._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class JourneyMongoRepositorySpec
  extends UnitSpec
    with MongoSpecSupport
    with ScalaFutures {

  "journeyExists" should {

    "return true if there is a journey for the given JourneyId" in new Setup {
      val journeyId = journeyIds.generateOne
      val relativeUrl = relativeUrls.generateOne

      repository.insert(journeyId -> relativeUrl).futureValue

      repository.journeyExists(journeyId).futureValue shouldBe true
    }

    "return false if there is no journey with the given JourneyId" in new Setup {
      val journeyId = journeyIds.generateOne
      repository.journeyExists(journeyId).futureValue shouldBe false
    }
  }

  private trait Setup {
    val repository = new JourneyMongoRepository(new ReactiveMongoComponent {
      override val mongoConnector: MongoConnector = mongoConnectorForTest
    })

    await(repository.removeAll())
  }
}
