package uk.gov.hmrc.personaldetailsvalidationfrontend.pages

import uk.gov.hmrc.personaldetailsvalidationfrontend.support.WebPage

object ErrorPage extends WebPage {
  override def isCurrentPage = pageTitle == "Sorry, we are experiencing technical difficulties"

  override val url = ""
}
