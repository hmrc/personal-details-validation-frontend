package uk.gov.hmrc.personaldetailsvalidation.support

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}

object SingletonDriver {

  val driver:WebDriver = Option(System.getProperty("browser")) match {
    case Some("chrome") => new ChromeDriver(new ChromeOptions)
    case _ => new FirefoxDriver()
  }
  def closeInstance(): Unit = driver.quit()
}
