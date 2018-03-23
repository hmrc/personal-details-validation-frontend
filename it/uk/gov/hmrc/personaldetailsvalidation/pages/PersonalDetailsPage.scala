package uk.gov.hmrc.personaldetailsvalidation.pages

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.support.{FormErrors, WebPage}

class PersonalDetailsPage private[PersonalDetailsPage](title: String, completionUrl: String) extends WebPage {

  val url: String = s"/personal-details-validation/personal-details?completionUrl=$completionUrl"

  def verifyThisPageDisplayed(): Unit = {
    pageTitle shouldBe title
    currentUrl.path shouldBe url.path
    currentUrl.query shouldBe url.query
  }

  def fillInWithNino(firstName: String, lastName: String, nino: Nino, dob: LocalDate): Unit = {
    textField("firstName").value = firstName
    textField("lastName").value = lastName
    textField("nino").value = nino.value
    numberField("dateOfBirth.day").value = dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value = dob.getMonthValue.toString
    numberField("dateOfBirth.year").value = dob.getYear.toString
  }

  def fillInWithPostcode(firstName: String, lastName: String, postCode: String, dob: LocalDate): Unit = {
    textField("firstName").value = firstName
    textField("lastName").value = lastName
    textField("postcode").value = postCode
    numberField("dateOfBirth.day").value = dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value = dob.getMonthValue.toString
    numberField("dateOfBirth.year").value = dob.getYear.toString
  }

  def verifyDataPresent(firstName: String, lastName: String, nino: Nino, dob: LocalDate): Unit = {
    textField("firstName").value shouldBe firstName
    textField("lastName").value shouldBe lastName
    textField("nino").value shouldBe nino.toString()
    numberField("dateOfBirth.day").value shouldBe dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value shouldBe dob.getMonthValue.toString
    numberField("dateOfBirth.year").value shouldBe dob.getYear.toString
  }

  def verifyDataBlank(): Unit = {
    textField("firstName").value shouldBe ""
    textField("lastName").value shouldBe ""
    textField("nino").value shouldBe ""
    numberField("dateOfBirth.day").value shouldBe ""
    numberField("dateOfBirth.month").value shouldBe ""
    numberField("dateOfBirth.year").value shouldBe ""
  }

  def submitForm(): Unit =
    find(cssSelector("button[type=submit]")) match {
      case Some(element) => click on element
      case _ => fail("Continue button not found")
    }
}

object PersonalDetailsPage {

  def personalDetailsPage(completionUrl: String): PersonalDetailsPage =
    new PersonalDetailsPage("Enter your details - Confirm your identity - GOV.UK", completionUrl)

  def personalDetailsErrorPage(completionUrl: String): PersonalDetailsPage with FormErrors =
    new PersonalDetailsPage("Error: Enter your details - Confirm your identity - GOV.UK", completionUrl) with FormErrors
}
