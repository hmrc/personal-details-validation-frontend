package uk.gov.hmrc.personaldetailsvalidationfrontend.specs

import uk.gov.hmrc.personaldetailsvalidationfrontend.pages.{ErrorPage, PersonalDetailsPage}

class PersonalDetailsValidationStartISpec extends BaseIntegrationSpec {

  "GET /personal-details-validation/start" should {
    "redirect to personal details page" in {
      goTo("/start?completionUrl=/foobar")
      on(PersonalDetailsPage)
    }

    "render error page if completionUrl is not provided" in {
      goTo("/start")
      on(ErrorPage)
    }

    "render error page if completionUrl is not relative" in {
      goTo("/start?completionUrl=http://foobar")
      on(ErrorPage)
    }
  }
}
