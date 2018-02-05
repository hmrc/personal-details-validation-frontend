package uk.gov.hmrc.http

import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters
import uk.gov.hmrc.play.test.UnitSpec

class PersonalDetailsValidationFiltersSpecs extends UnitSpec with MockFactory {

  "filters" should {
    "include filters from FrontendFilters and AddGaUserIdInHeaderFilter" in new Setup {
      personalDetailsValidationFilters.filters shouldBe originalFilters :+ addGaUserIdInHeaderFilter
    }
  }

  trait Setup {

    val filter1 = mock[EssentialFilter]
    val filter2 = mock[EssentialFilter]
    val filter3 = mock[EssentialFilter]
    val filter4 = mock[EssentialFilter]
    val addGaUserIdInHeaderFilter = mock[AddGaUserIdInHeaderFilter]

    val originalFilters = Seq(filter1, filter2, filter3, filter4)

    val configuration = Configuration.from(Map("security.headers.filter.enabled" -> false))

    val frontendFilters = new FrontendFilters(configuration, null, null, null, null, null, null,null, null, null, null){
      override val filters = originalFilters
    }

    val personalDetailsValidationFilters = new PersonalDetailsValidationFilters(frontendFilters, addGaUserIdInHeaderFilter)
  }

}
