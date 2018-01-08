package uk.gov.hmrc.personaldetailsvalidation.pages

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class PersonalDetailsPage(completionUrl: String) extends WebPage {

  val url: String = s"/personal-details-validation/personal-details?completionUrl=$completionUrl"

  def verifyThisPageDisplayed(): Unit = {
    pageTitle shouldBe "Enter your details - Confirm your identity"
    currentUrl.path shouldBe url.path
    currentUrl.query shouldBe url.query
  }

  def fillIn(firstName: String, lastName: String, nino: Nino, dob: LocalDate): Unit = {
    textField("firstName").value = firstName
    textField("lastName").value = lastName
    textField("nino").value = nino.toString()
    numberField("dateOfBirth.day").value = dob.getDayOfMonth.toString
    numberField("dateOfBirth.month").value = dob.getMonthValue.toString
    numberField("dateOfBirth.year").value = dob.getYear.toString
  }

  def submitForm(): Unit =
    find(cssSelector("button[type=submit]")) match {
      case Some(element) => click on element
      case _ => fail("Continue button not found")
    }
}
