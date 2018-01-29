package uk.gov.hmrc.personaldetailsvalidation.specs

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model.{NonEmptyString, PersonalDetails}
import uk.gov.hmrc.personaldetailsvalidation.pages.{CompletionPage, ErrorPage, PersonalDetailsPage}
import uk.gov.hmrc.personaldetailsvalidation.services.PersonalDetailsService
import uk.gov.hmrc.personaldetailsvalidation.support.BaseIntegrationSpec
import uk.gov.hmrc.personaldetailsvalidation.support.wiremock.WiremockedService

class PersonalDetailsPageISpec
  extends BaseIntegrationSpec
    with WiremockedService {

  wiremock("personal-details-validation").on("localhost", 11111)

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

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      val completionUrl = "/foobar?param1=value1&param2=value2"
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I fill in the fields with valid data")
      personalDetailsPage.fillIn("Jim", "Ferguson", Nino("AA000003D"), LocalDate.of(1948, 4, 23))

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesSuccessfully PersonalDetails(
        firstName = NonEmptyString("Jim"),
        lastName = NonEmptyString("Ferguson"),
        nino = Nino("AA000003D"),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      And("when I submit the data")
      personalDetailsPage.submitForm()

      Then("I should get redirected to my completion url")
      on(CompletionPage(completionUrl))
    }

    scenario("Personal Details page submitted with invalid personal details") {

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      val completionUrl = "/completion-url"
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I submit some invalid data")
      personalDetailsPage.fillIn(" ", "Ferguson", Nino("AA999999D"), LocalDate.of(1948, 4, 23))
      personalDetailsPage.submitForm()

      Then("I should stay on the Personal Details page")
      on(personalDetailsPage)

      And("I should still see the data I entered")
      personalDetailsPage.verifyDataPresent(" ", "Ferguson", Nino("AA999999D"), LocalDate.of(1948, 4, 23))

      And("I should see errors for invalid values")
      personalDetailsPage containsErrors "Enter your first name."
    }
  }
}
