package uk.gov.hmrc.personaldetailsvalidation.support

import org.openqa.selenium.{By, WebDriver}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.selenium.WebBrowser
import org.scalatest.{Assertions, Matchers}

trait NavigationSugar extends WebBrowser with Eventually with Assertions with Matchers with IntegrationPatience {

  def goOn(page: WebPage)(implicit webDriver: WebDriver): Unit = {
    goTo(page)
    on(page)
  }

  def on(page: WebPage)(implicit webDriver: WebDriver): Unit = {
    setCaptureDir("target/screenshots")
    withScreenshot {
      withClue("timeout waiting for the 'body' element") {
        eventually {
          webDriver.findElement(By.tagName("body"))
        }
      }

      page.verifyThisPageDisplayed()
    }
  }
}
