package uk.gov.hmrc.personaldetailsvalidationfrontend.pages

import uk.gov.hmrc.personaldetailsvalidationfrontend.support.WebPage

object PersonalDetailsPage extends WebPage {
  override val url: String = ""

  override def isCurrentPage: Boolean = pageTitle == "Enter your details - Confirm your identity"

}
