package uk.gov.hmrc.personaldetailsvalidation.specs

import uk.gov.hmrc.personaldetailsvalidation.pages.{ErrorPage, PersonalDetailsPage}

class PersonalDetailsValidationStartISpec extends BaseIntegrationSpec {

  feature("Start Personal Details Validation journey") {

    scenario("Personal Details Validation journey started with a valid completionUrl") {

      When("I navigate to /personal-details-validation/start with a valid completionUrl")
      val completionUrl = "/foobar"
      goTo(s"/start?completionUrl=$completionUrl")

      Then("I should get redirected to the Personal Details page")
      on(PersonalDetailsPage(completionUrl))
    }

    scenario("Personal Details Validation journey started without a completionUrl parameter") {

      When("I navigate to /personal-details-validation/start without a completionUrl parameter")
      goTo("/start")

      Then("I should see the error page")
      on(ErrorPage())
    }

    scenario("Personal Details Validation journey started with an invalid completionUrl") {

      When("I navigate to /personal-details-validation/start with an invalid completionUrl")
      goTo("/start?completionUrl=http://foobar")

      Then("I should see the error page")
      on(ErrorPage())
    }
  }
}
