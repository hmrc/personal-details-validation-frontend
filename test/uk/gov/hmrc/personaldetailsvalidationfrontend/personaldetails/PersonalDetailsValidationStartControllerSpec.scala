package uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails

import org.scalatest.concurrent.ScalaFutures
import play.mvc.Http.HeaderNames.LOCATION
import uk.gov.hmrc.personaldetailsvalidationfrontend.test.controllers.EndpointSetup
import uk.gov.hmrc.play.test.UnitSpec

class PersonalDetailsValidationStartControllerSpec extends UnitSpec with ScalaFutures {

  "start" should {
    "redirect to personal details page" in new Setup {
      val result = controller.start("foobar")(request).futureValue
      status(result) shouldBe 303

      result.header.headers(LOCATION) should startWith("/personal-details?journeyId=")
    }
  }

  trait Setup extends EndpointSetup {
    val controller = new PersonalDetailsValidationStartController
  }

}
