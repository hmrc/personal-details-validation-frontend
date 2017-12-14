package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

object ErrorPage extends WebPage {
  override def isCurrentPage = pageTitle == "Sorry, we are experiencing technical difficulties"

  override val url = ""
}
