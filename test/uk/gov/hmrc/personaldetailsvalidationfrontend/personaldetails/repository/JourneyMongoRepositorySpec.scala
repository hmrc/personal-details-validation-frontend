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

  "insert" should {

    "allow to insert given JourneyId and RelativeUrl" in new Setup {
      val journeyId = journeyIds.generateOne
      val relativeUrl = relativeUrls.generateOne

      repository.insert(journeyId -> relativeUrl).futureValue

      repository.journeyExists(journeyId).futureValue shouldBe true
    }
  }

  private trait Setup {
    val repository = new JourneyMongoRepository(new ReactiveMongoComponent {
      override val mongoConnector: MongoConnector = mongoConnectorForTest
    })

    await(repository.removeAll())
  }
}
