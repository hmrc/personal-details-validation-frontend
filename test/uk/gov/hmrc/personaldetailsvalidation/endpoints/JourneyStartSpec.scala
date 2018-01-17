package uk.gov.hmrc.personaldetailsvalidation.endpoints

import cats.Id
import generators.Generators.Implicits._
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.play.test.UnitSpec

class JourneyStartSpec extends UnitSpec {

  "findRedirect" should {

    "return redirect to the GET /personal-details" in new Setup {
      journeyStart.findRedirect(completionUrl) shouldBe Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl))
    }
  }

  private trait Setup {
    val completionUrl = ValuesGenerators.completionUrls.generateOne

    val journeyStart = new JourneyStart[Id]()
  }
}
