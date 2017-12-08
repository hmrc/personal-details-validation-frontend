package uk.gov.hmrc.personaldetailsvalidationfrontend.support

import org.openqa.selenium.WebDriver

trait ImplicitWebDriverSugar {
  implicit lazy val webDriver: WebDriver = SingletonDriver.driver
}
