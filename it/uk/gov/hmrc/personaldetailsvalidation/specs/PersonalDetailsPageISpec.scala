package uk.gov.hmrc.personaldetailsvalidation.specs

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.pages.{CompletionPage, ErrorPage, PersonalDetailsPage}

class PersonalDetailsPageISpec extends BaseIntegrationSpec {

  feature("Personal Details Page") {

    scenario("Personal Details page accessed without a valid completionUrl") {

      When("I navigate to /personal-details-validation/personal-details without completionUrl")
      goTo("/personal-details")

      Then("I should see the error page")
      on(ErrorPage())
    }

    scenario("Personal Details page accessed without a invalid completionUrl") {

      When("I navigate to /personal-details-validation/personal-details with invalid completionUrl")
      goTo("/personal-details?completionUrl=http://host")

      Then("I should see the error page")
      on(ErrorPage())
    }

    scenario("Personal Details page submitted with valid personal details") {

      When("I navigate to /personal-details-validation/personal-details with invalid completionUrl")
      val completionUrl = "/completion-url"
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I fill in the fields with valid data")
      personalDetailsPage.fillIn("Jim", "Ferguson", Nino("AA000003D"), LocalDate.of(1948, 4, 23))

      And("submit it")
      personalDetailsPage.submitForm()

      Then("I should get redirected to the completion url I gave")
      on(CompletionPage(completionUrl))
    }

    scenario("Personal Details page submitted with invalid personal details") {

      When("I navigate to /personal-details-validation/personal-details with invalid completionUrl")
      val completionUrl = "/completion-url"
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I submit it without any data entered")
      personalDetailsPage.submitForm()

      Then("I should stay on the Personal Details page")
      on(personalDetailsPage)
    }
  }
}
