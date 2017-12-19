package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class PersonalDetailsPage(completionUrl: String) extends WebPage {

  val url: String = s"/personal-details?completionUrl=$completionUrl"

  lazy val verifyDisplayed: () => Unit = () => {
    pageTitle shouldBe "Enter your details - Confirm your identity"
    currentUrl shouldBe url
  }
}
