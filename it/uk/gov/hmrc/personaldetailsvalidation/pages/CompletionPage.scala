package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class CompletionPage(url: String) extends WebPage {

  def verifyThisPageDisplayed(): Unit = {
    currentUrl.path shouldBe url.path
    currentUrl.query should include("validationId")
  }
}
