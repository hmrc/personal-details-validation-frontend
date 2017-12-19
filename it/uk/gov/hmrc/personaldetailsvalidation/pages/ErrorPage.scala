package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class ErrorPage(url: String = "") extends WebPage {

  override lazy val isCurrentPage: Boolean =
    pageTitle == "Sorry, we are experiencing technical difficulties"
}
