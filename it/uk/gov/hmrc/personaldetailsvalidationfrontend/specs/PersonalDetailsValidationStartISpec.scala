package uk.gov.hmrc.personaldetailsvalidationfrontend.specs

import java.util.UUID

import uk.gov.hmrc.personaldetailsvalidationfrontend.pages.{ErrorPage, PersonalDetailsPage}

class PersonalDetailsValidationStartISpec extends BaseIntegrationSpec {

  feature("Personal Details validation journey") {

    scenario("Personal Details validation journey started with a valid completionUrl") {

      When("I navigate to /personal-details-validation/start with a valid completionUrl")
      goTo("/start?completionUrl=/foobar")

      Then("I should get redirected to the Personal Details page")
      on(PersonalDetailsPage)
    }

    scenario("Personal Details validation page accessed without a valid journeyId") {

      When("I navigate to /personal-details-validation/personal-details with a non-existing journeyId")
      goTo(s"/personal-details?journeyId=${UUID.randomUUID()}")

      Then("I should see the error page")
      on(ErrorPage)
    }

    scenario("Personal Details validation journey started without a completionUrl parameter") {

      When("I navigate to /personal-details-validation/start without a completionUrl parameter")
      goTo("/start")

      Then("I should see the error page")
      on(ErrorPage)
    }

    scenario("Personal Details validation journey started with an invalid completionUrl") {

      When("I navigate to /personal-details-validation/start with an invalid completionUrl")
      goTo("/start?completionUrl=http://foobar")

      Then("I should see the error page")
      on(ErrorPage)
    }
  }
}
