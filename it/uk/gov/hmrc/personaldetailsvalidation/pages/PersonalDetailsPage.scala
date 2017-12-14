package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

object PersonalDetailsPage extends WebPage {
  override val url: String = ""

  override def isCurrentPage: Boolean = pageTitle == "Enter your details - Confirm your identity"

}
