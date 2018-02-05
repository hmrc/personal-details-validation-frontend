package uk.gov.hmrc.http

import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec

class PlayHttpFilterConfigurationSpecs extends UnitSpec with OneAppPerSuite {

  "play application configuration" should {
    "point 'play.http.filters' to PersonalDetailsValidationFilters" in {

      val currentConfiguration = fakeApplication.injector.instanceOf[Configuration]

      currentConfiguration.getString("play.http.filters") shouldBe Some("uk.gov.hmrc.http.PersonalDetailsValidationFilters")

    }
  }

}
