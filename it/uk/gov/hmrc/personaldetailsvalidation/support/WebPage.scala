package uk.gov.hmrc.personaldetailsvalidation.support

import org.scalatest.Matchers
import org.scalatest.selenium.{Page, WebBrowser}

trait WebPage extends Page with WebBrowser with Matchers with ImplicitWebDriverSugar {

  def isCurrentPage: Boolean
}
