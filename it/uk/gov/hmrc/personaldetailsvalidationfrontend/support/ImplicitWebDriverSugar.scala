package uk.gov.hmrc.personaldetailsvalidationfrontend.support

import org.openqa.selenium.WebDriver
import uk.gov.hmrc.integration.framework.SingletonDriver

trait ImplicitWebDriverSugar {
  System.setProperty("browser", "chrome")
  implicit lazy val webDriver: WebDriver = SingletonDriver.getInstance()
}
