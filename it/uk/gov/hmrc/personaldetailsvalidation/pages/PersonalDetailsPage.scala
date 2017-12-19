package uk.gov.hmrc.personaldetailsvalidation.pages

import uk.gov.hmrc.personaldetailsvalidation.support.WebPage

case class PersonalDetailsPage(url: String = "") extends WebPage {
  override lazy val isCurrentPage: Boolean =
    pageTitle == "Enter your details - Confirm your identity"

}
