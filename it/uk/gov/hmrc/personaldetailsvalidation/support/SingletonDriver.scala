package uk.gov.hmrc.personaldetailsvalidation.support

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver

object SingletonDriver {
  val driver: WebDriver = new FirefoxDriver()

  def closeInstance() = driver.quit()
}
