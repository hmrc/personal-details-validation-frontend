package uk.gov.hmrc.personaldetailsvalidation.specs

import java.net.URLEncoder
import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model.NonEmptyString
import uk.gov.hmrc.personaldetailsvalidation.pages.{CompletionPage, ErrorPage, PersonalDetailsPage}
import uk.gov.hmrc.personaldetailsvalidation.services.PersonalDetailsService
import uk.gov.hmrc.personaldetailsvalidation.services.PersonalDetailsService.PersonalDetailsData
import uk.gov.hmrc.personaldetailsvalidation.support.{BaseIntegrationSpec, FormErrors}
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

    scenario("validation successful when personal Details page submitted with valid personal details containing nino") {

      val testData = PersonalDetailsData(
        firstName = NonEmptyString("Jim").value,
        lastName = NonEmptyString("Ferguson").value,
        nino = Some(Nino("AA000003D")),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I fill in the fields with valid data")
      personalDetailsPage.fillInWithNino(testData.firstName, testData.lastName, testData.nino.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesSuccessfully testData

      And("when I submit the data")
      personalDetailsPage.submitForm()

      Then("I should get redirected to my completion url")
      on(CompletionPage(completionUrl))
    }

    scenario("validation failed when personal Details page submitted with valid personal details containing nino") {

      val testData = PersonalDetailsData(
        firstName = NonEmptyString("Jim").value,
        lastName = NonEmptyString("Ferguson").value,
        nino = Some(Nino("AA000003D")),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I fill in the fields with valid data")
      personalDetailsPage.fillInWithNino(testData.firstName, testData.lastName, testData.nino.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesUnsuccessfully testData

      And("when I submit the data")
      personalDetailsPage.submitForm()

      Then("I should see the Personal Details error page")
      val personalDetailsErrorPage = new PersonalDetailsPage(completionUrl, true) with FormErrors
      on(personalDetailsErrorPage)

      And("I should not see the data I entered")
      personalDetailsErrorPage.verifyDataBlank()

      And("I should see errors")
      personalDetailsErrorPage.summaryErrors shouldBe List(
        "The information you've entered doesn't match our records." +
          " Check your details and try again."
      )
      personalDetailsErrorPage.fieldErrors shouldBe Map.empty
    }

    scenario("validation successful when personal Details page submitted with valid personal details containing postcode") {

      pending

      val testData = PersonalDetailsData(
        firstName = NonEmptyString("Jim").value,
        lastName = NonEmptyString("Ferguson").value,
        postcode = Some("AA00 03D"),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I fill in the fields with valid data")
      personalDetailsPage.fillInWithPostcode(testData.firstName, testData.lastName, testData.postcode.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesSuccessfully testData

      And("when I submit the data")
      personalDetailsPage.submitForm()

      Then("I should get redirected to my completion url")
      on(CompletionPage(completionUrl))
    }

    scenario("validation failed when personal Details page submitted with valid personal details containing postcode") {

      pending

      val testData = PersonalDetailsData(
        firstName = NonEmptyString("Jim").value,
        lastName = NonEmptyString("Ferguson").value,
        postcode = Some("AA00 03D"),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I fill in the fields with valid data")
      personalDetailsPage.fillInWithPostcode(testData.firstName, testData.lastName, testData.postcode.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesUnsuccessfully testData

      And("when I submit the data")
      personalDetailsPage.submitForm()

      Then("I should see the Personal Details error page")
      val personalDetailsErrorPage = new PersonalDetailsPage(completionUrl, true) with FormErrors
      on(personalDetailsErrorPage)

      And("I should not see the data I entered")
      personalDetailsErrorPage.verifyDataBlank()

      And("I should see errors")
      personalDetailsErrorPage.summaryErrors shouldBe List(
        "The information you've entered doesn't match our records." +
          " Check your details and try again."
      )
      personalDetailsErrorPage.fieldErrors shouldBe Map.empty
    }

    scenario("Personal Details page submitted with invalid personal details") {

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val personalDetailsPage = PersonalDetailsPage(completionUrl)
      on(personalDetailsPage)

      When("I submit some invalid data")
      personalDetailsPage.fillInWithNino(" ", " ", Nino("AA000003C"), LocalDate.of(1948, 12, 23))
      personalDetailsPage.submitForm()

      Then("I should see the Personal Details error page")
      val personalDetailsErrorPage = new PersonalDetailsPage(completionUrl, true) with FormErrors
      on(personalDetailsErrorPage)

      And("I should still see the data I entered")
      personalDetailsErrorPage.verifyDataPresent(" ", " ", Nino("AA000003C"), LocalDate.of(1948, 12, 23))

      And("I should see errors for invalid values")
      personalDetailsErrorPage.summaryErrors shouldBe List("Enter your first name.", "Enter your last name.")
      personalDetailsErrorPage.fieldErrors shouldBe Map(
        "firstName" -> "Enter your first name.",
        "lastName" -> "Enter your last name."
      )
    }
  }

  val completionUrl = URLEncoder.encode("/foobar?param1=value1&param2=value2", "utf-8")

}
