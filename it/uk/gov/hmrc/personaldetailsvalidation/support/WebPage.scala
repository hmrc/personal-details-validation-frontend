package uk.gov.hmrc.personaldetailsvalidation.support

import java.net.URI

import org.scalatest.Matchers
import org.scalatest.selenium.{Page, WebBrowser}

trait WebPage extends Page with WebBrowser with Matchers with ImplicitWebDriverSugar {

  def verifyThisPageDisplayed(): Unit

  protected implicit class StringUrlOps(stringUrl: String) {
    private lazy val uri = new URI(stringUrl)

    lazy val path: String = uri.getPath
    lazy val query: String = uri.getQuery
  }
}
