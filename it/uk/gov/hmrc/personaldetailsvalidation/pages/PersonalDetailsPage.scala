package uk.gov.hmrc.personaldetailsvalidation.pages

import java.net.URL
import java.time.LocalDate

import org.openqa.selenium.WebElement
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.support.{FormErrors, WebPage}

abstract class PersonalDetailsPage(title: String, val completionUrl: String) extends WebPage {

  def verifyThisPageDisplayed(): Unit = {
    pageTitle shouldBe title
    currentUrl.path shouldBe url.path
    currentUrl.query shouldBe url.query
  }

  protected def verifyOtherFields(): Unit

  def verifyDataBlank(): Unit = {
    textField("firstName").value shouldBe ""
    textField("lastName").value shouldBe ""
    numberField("dateOfBirth.day").value shouldBe ""
    numberField("dateOfBirth.month").value shouldBe ""
    numberField("dateOfBirth.year").value shouldBe ""
    verifyOtherFields()
  }

  def submitForm(): Unit =
    find(cssSelector("button[type=submit]")) match {
      case Some(element) => click on element
      case _ => fail("Continue button not found")
    }

  def exitLinkToCompletionUrlExists(completionUrl: String): Boolean = {
    find(cssSelector(s".error-summary a[href='$completionUrl']")) match {
      case Some(_) => true
      case _ => false
    }
  }
}

class PersonalDetailsNinoPage (title: String, override val completionUrl: String) extends PersonalDetailsPage(title, completionUrl) {
  override val url: String = s"/personal-details-validation/personal-details?completionUrl=$completionUrl"

  def fillInWithNino(firstName: String, lastName: String, nino: Nino, dob: LocalDate): Unit = {
    textField("firstName").value = firstName
    textField("lastName").value = lastName
    textField("nino").value = nino.value
    numberField("dateOfBirth.day").value = dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value = dob.getMonthValue.toString
    numberField("dateOfBirth.year").value = dob.getYear.toString
  }

  def verifyNinoDataPresent(firstName: String, lastName: String, nino: Nino, dob: LocalDate): Unit = {
    textField("firstName").value shouldBe firstName
    textField("lastName").value shouldBe lastName
    textField("nino").value shouldBe nino.toString()
    numberField("dateOfBirth.day").value shouldBe dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value shouldBe dob.getMonthValue.toString
    numberField("dateOfBirth.year").value shouldBe dob.getYear.toString
  }

  protected override def verifyOtherFields = textField("nino").value shouldBe ""

  def selectPostcodeOption(): PersonalDetailsPostcodePage = {
    find(cssSelector("a[href*='postcodeVersion=true']")) match {
      case Some(text) => click on text
      case _ => fail("postcode option not found")
    }
    new PersonalDetailsPostcodePage(title, completionUrl)
  }
}

class PersonalDetailsPostcodePage (title: String, override val completionUrl: String) extends PersonalDetailsPage(title, completionUrl) {
  override val url: String = s"/personal-details-validation/personal-details?completionUrl=$completionUrl&postcodeVersion=true"

  def fillInWithPostcode(firstName: String, lastName: String, postCode: String, dob: LocalDate): Unit = {
    textField("firstName").value = firstName
    textField("lastName").value = lastName
    textField("postcode").value = postCode
    numberField("dateOfBirth.day").value = dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value = dob.getMonthValue.toString
    numberField("dateOfBirth.year").value = dob.getYear.toString
  }

  protected override def verifyOtherFields = textField("postcode").value shouldBe ""

  def verifyPostcodeDataPresent(firstName: String, lastName: String, postcode: String, dob: LocalDate): Unit = {
    textField("firstName").value shouldBe firstName
    textField("lastName").value shouldBe lastName
    textField("postcode").value shouldBe postcode
    numberField("dateOfBirth.day").value shouldBe dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value shouldBe dob.getMonthValue.toString
    numberField("dateOfBirth.year").value shouldBe dob.getYear.toString
  }
}

object PersonalDetailsPage {

  def personalDetailsPage(completionUrl: String): PersonalDetailsNinoPage =
    new PersonalDetailsNinoPage("Enter your details - Confirm your identity - GOV.UK", completionUrl)

  def personalDetailsNinoErrorPage(completionUrl: String): PersonalDetailsNinoPage with FormErrors =
    new PersonalDetailsNinoPage("Error: Enter your details - Confirm your identity - GOV.UK", completionUrl) with FormErrors

  def personalDetailsPostcodeErrorPage(completionUrl: String): PersonalDetailsPostcodePage with FormErrors =
    new PersonalDetailsPostcodePage("Error: Enter your details - Confirm your identity - GOV.UK", completionUrl) with FormErrors
}
