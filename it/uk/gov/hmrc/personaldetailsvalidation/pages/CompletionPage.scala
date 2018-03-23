package uk.gov.hmrc.personaldetailsvalidation.pages

import java.net.URLDecoder

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class CompletionPage(url: String) extends WebPage {

  def verifyThisPageDisplayed(): Unit = {
    currentUrl.path shouldBe URLDecoder.decode(url, "utf-8").path
    currentUrl.query should include("validationId")
  }
}
