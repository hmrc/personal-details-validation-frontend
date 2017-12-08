package uk.gov.hmrc.personaldetailsvalidationfrontend.support

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.remote.DesiredCapabilities

object SingletonDriver {
  lazy val driver = new ChromeDriver(DesiredCapabilities.chrome())

  def closeInstance() = driver.quit()
}
