package uk.gov.hmrc.personaldetailsvalidation.specs

import java.net.{URLDecoder, URLEncoder}
import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model.NonEmptyString
import uk.gov.hmrc.personaldetailsvalidation.pages.{CompletionPage, ErrorPage}
import uk.gov.hmrc.personaldetailsvalidation.pages.PersonalDetailsPage._
import uk.gov.hmrc.personaldetailsvalidation.services.PersonalDetailsService
import uk.gov.hmrc.personaldetailsvalidation.services.PersonalDetailsService.PersonalDetailsData
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
      val page = personalDetailsPage(completionUrl)
      on(page)

      When("I fill in the fields with valid data")
      page.fillInWithNino(testData.firstName, testData.lastName, testData.nino.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesSuccessfully testData

      And("when I submit the data")
      page.submitForm()

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
      val page = personalDetailsPage(completionUrl)
      on(page)

      When("I fill in the fields with invalid data")
      page.fillInWithNino(testData.firstName, testData.lastName, testData.nino.get, testData.dateOfBirth)

      And("I know the personal-details-validation service does not validate the data successfully")
      PersonalDetailsService validatesUnsuccessfully testData

      And("when I submit the data")
      page.submitForm()

      Then("I should see the Personal Details error page")
      val errorPage = personalDetailsNinoErrorPage(completionUrl)
      on(errorPage)

      And("I should not see the data I entered")
      errorPage.verifyDataBlank()

      And("I should see errors")
      errorPage.summaryErrorsHeading shouldBe "There is a problem"
      errorPage.summaryErrors shouldBe List(
       "We couldn't find any records that match the details you entered. " +
         "Please try again, checking that all your details are correct, or contact HMRC to get help"
      )
      errorPage.fieldErrors shouldBe Map.empty

      And("The error summary contains an exit link for the first ValidationId")
      val decodedUrl = URLDecoder.decode(completionUrl, "UTF-8")
      errorPage.exitLinkToCompletionUrlExists(decodedUrl) shouldBe true
    }

    scenario("validation successful when personal Details page submitted with valid personal details containing postcode") {

      val testData = PersonalDetailsData(
        firstName = NonEmptyString("Jim").value,
        lastName = NonEmptyString("Ferguson").value,
        postcode = Some("LE2 6JP"),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      And("I select the 'I don't have a National Insurance number' option")
      val page = personalDetailsPage(completionUrl)
      val postcodePage = page.selectPostcodeOption()

      Then("I should see the Personal Details page")
      on(postcodePage)

      When("I fill in the fields with valid data")
      postcodePage.fillInWithPostcode(testData.firstName, testData.lastName, testData.postcode.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesSuccessfully testData

      And("when I submit the data")
      postcodePage.submitForm()

      Then("I should get redirected to my completion url")
      on(CompletionPage(completionUrl))
    }

    scenario("Personal Details page submitted with invalid postcode details") {

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      And("I select the 'I don't have a National Insurance number' option")
      val page = personalDetailsPage(completionUrl)
      val postcodePage = page.selectPostcodeOption()

      Then("I should see the Personal Details page")
      on(postcodePage)

      When("I submit some invalid data")
      postcodePage.fillInWithPostcode("Jim", "Ferguson", "some-invalid-postcode", LocalDate.of(1948, 4, 23))
      postcodePage.submitForm()

      Then("I should see the Personal Details page")
      val errorPage = personalDetailsPostcodeErrorPage(postcodePage.completionUrl)
      on(errorPage)

      Then("I should stay on the Personal Details page")
      on(errorPage)

      And("I should still see the data I entered")
      errorPage.verifyPostcodeDataPresent("Jim", "Ferguson", "some-invalid-postcode", LocalDate.of(1948, 4, 23))
    }

    scenario("validation failed when personal Details page submitted with valid personal details containing postcode") {

      val testData = PersonalDetailsData(
        firstName = NonEmptyString("Jim").value,
        lastName = NonEmptyString("Ferguson").value,
        postcode = Some("LE2 6JP"),
        dateOfBirth = LocalDate.of(1948, 4, 23)
      )

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      And("I select the 'I don't have a National Insurance number' option")
      val page = personalDetailsPage(completionUrl)
      val postcodePage = page.selectPostcodeOption()

      Then("I should see the Personal Details page")
      on(postcodePage)

      When("I fill in the fields with valid data")
      postcodePage.fillInWithPostcode(testData.firstName, testData.lastName, testData.postcode.get, testData.dateOfBirth)

      And("I know the personal-details-validation service validates the data successfully")
      PersonalDetailsService validatesUnsuccessfully testData

      And("when I submit the data")
      page.submitForm()

      Then("I should see the Personal Details error page")
      val errorPage = personalDetailsPostcodeErrorPage(completionUrl)
      on(errorPage)

      And("I should not see the data I entered")
      errorPage.verifyDataBlank()

      And("I should see errors")
      errorPage.summaryErrorsHeading shouldBe "There is a problem"
      errorPage.summaryErrors shouldBe List(
        "The information you've entered doesn't match our records." +
          " Check your details and try again."
      )
      errorPage.fieldErrors shouldBe Map.empty
      And("The error summary contains an exit link for the first ValidationId")
      val decodedUrl = URLDecoder.decode(completionUrl, "UTF-8")
      errorPage.exitLinkToCompletionUrlExists(decodedUrl) shouldBe true
    }

    scenario("Personal Details page submitted with invalid personal details") {

      When("I navigate to /personal-details-validation/personal-details with valid completionUrl")
      goTo(s"/personal-details?completionUrl=$completionUrl")

      Then("I should see the Personal Details page")
      val page = personalDetailsPage(completionUrl)
      on(page)

      When("I submit some invalid data")
      page.fillInWithNino(" ", " ", Nino("AA000003C"), LocalDate.of(1948, 12, 23))
      page.submitForm()

      Then("I should see the Personal Details error page")
      val errorPage = personalDetailsNinoErrorPage(completionUrl)
      on(errorPage)

      And("I should still see the data I entered")
      errorPage.verifyNinoDataPresent(" ", " ", Nino("AA000003C"), LocalDate.of(1948, 12, 23))

      And("I should see errors for invalid values")
      errorPage.summaryErrors shouldBe List("Enter your first name.", "Enter your last name.")
      errorPage.fieldErrors shouldBe Map(
        "firstName" -> "Enter your first name.",
        "lastName" -> "Enter your last name."
      )
    }
  }

  val completionUrl = URLEncoder.encode("/foobar?param1=value1&param2=value2", "utf-8")

}
