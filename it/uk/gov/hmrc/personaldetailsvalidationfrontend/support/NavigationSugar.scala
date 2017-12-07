package uk.gov.hmrc.personaldetailsvalidationfrontend.support

import org.openqa.selenium.{By, WebDriver}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.selenium.WebBrowser
import org.scalatest.{Assertions, Matchers}

trait NavigationSugar extends WebBrowser with Eventually with Assertions with Matchers with IntegrationPatience {

  def goOn(page: WebPage)(implicit webDriver: WebDriver) = {
    goTo(page)
    on(page)
  }

  def on(page: WebPage)(implicit webDriver: WebDriver) = {
    setCaptureDir("target/screenshots")
    withScreenshot {
      withClue("timeout waiting for the 'body' element") {
        eventually {
          webDriver.findElement(By.tagName("body"))
        }
      }

      withClue(s"Page isCurrentPage returned false: browser's current url: [$currentUrl], actual title in html: $pageTitle was not equal to expected page object's url [${page.url}]. Page source: $pageSource") {
        eventually {
          page.isCurrentPage shouldBe true
        }
      }
    }
  }
}
