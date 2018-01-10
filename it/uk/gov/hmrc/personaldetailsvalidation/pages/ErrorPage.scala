package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class ErrorPage(url: String = "") extends WebPage {

  def verifyThisPageDisplayed(): Unit =
    pageTitle shouldBe "Sorry, we are experiencing technical difficulties"
}
