package uk.gov.hmrc.personaldetailsvalidation.specs

import java.util.UUID

import uk.gov.hmrc.personaldetailsvalidation.pages.ErrorPage

class PersonalDetailsPageISpec extends BaseIntegrationSpec {

  feature("Personal Details Page") {

    scenario("Personal Details page accessed without a valid journeyId") {

      When("I navigate to /personal-details-validation/personal-details with a non-existing journeyId")
      goTo(s"/personal-details?journeyId=${UUID.randomUUID()}")

      Then("I should see the error page")
      on(ErrorPage)
    }
  }
}
