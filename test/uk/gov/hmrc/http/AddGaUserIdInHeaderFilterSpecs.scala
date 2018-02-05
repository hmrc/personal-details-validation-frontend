package uk.gov.hmrc.http

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{Cookie, RequestHeader, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderNames.googleAnalyticUserId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AddGaUserIdInHeaderFilterSpecs extends UnitSpec with MockFactory {

  "filter" should {
    "retain ga user id in request header if no ga cookie present" in new Setup {
      val request = FakeRequest().withHeaders(googleAnalyticUserId -> gaUserId, extraHeader)

      actionFunction.apply _ expects argAssert { requestHeader: RequestHeader =>
        requestHeader.headers.get(googleAnalyticUserId) shouldBe Some(gaUserId)
        requestHeader.headers.get(extraHeaderKey) shouldBe Some(extraHeaderValue)
      }

      filter(actionFunction)(request)
    }

    "overwrite ga user id in request header if ga cookie present" in new Setup {
      val improvedGaUserId: String = gaUserId.concat("improved")
      val request = FakeRequest().withHeaders(googleAnalyticUserId -> gaUserId, extraHeader).withCookies(Cookie("_ga", improvedGaUserId))

      actionFunction.apply _ expects argAssert { requestHeader: RequestHeader =>
        requestHeader.headers.get(googleAnalyticUserId) shouldBe Some(improvedGaUserId)
        requestHeader.headers.get(extraHeaderKey) shouldBe Some(extraHeaderValue)
      }

      filter(actionFunction)(request)
    }

    "keep ga user id in request header empty if already empty and no ga cookie present" in new Setup {
      val request = FakeRequest().withHeaders(extraHeader)

      actionFunction.apply _ expects argAssert { requestHeader: RequestHeader =>
        requestHeader.headers.get(googleAnalyticUserId) shouldBe None
        requestHeader.headers.get(extraHeaderKey) shouldBe Some(extraHeaderValue)
      }

      filter(actionFunction)(request)
    }
  }

  trait Setup {

    private implicit val system = ActorSystem()
    private implicit val mat = ActorMaterializer()

    val filter = new AddGaUserIdInHeaderFilter
    val gaUserId = "some-ga-user-id"

    val extraHeaderKey = "extra-header-key"
    val extraHeaderValue = "extra-header-value"
    val extraHeader = extraHeaderKey -> extraHeaderValue

    val actionFunction = mock[(RequestHeader) => Future[Result]]
  }

}
