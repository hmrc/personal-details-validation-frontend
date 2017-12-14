package uk.gov.hmrc.personaldetailsvalidationfrontend.support

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

object SingletonDriver {
  val driver: WebDriver = new FirefoxDriver()

  def closeInstance() = driver.quit()
}
