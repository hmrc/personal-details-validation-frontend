package uk.gov.hmrc.personaldetailsvalidationfrontend.support

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.remote.DesiredCapabilities

object SingletonDriver {
  lazy val driver = {
    val capabilities = DesiredCapabilities.chrome()

    val options = new ChromeOptions()
    options.merge(capabilities)

    new ChromeDriver(options)
  }

  def closeInstance() = driver.quit()
}

trait ImplicitWebDriverSugar {
  implicit lazy val webDriver: WebDriver = SingletonDriver.driver
}
