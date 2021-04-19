package uk.gov.hmrc.personaldetailsvalidation.support

import java.net.URL

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}

object SingletonDriver {
  import scala.util.Properties

  lazy val firefoxDriver = {
    val firefoxCapabilities: DesiredCapabilities = DesiredCapabilities.firefox()
    firefoxCapabilities.setJavascriptEnabled(true)
    firefoxCapabilities.setAcceptInsecureCerts(true)
    val firefoxOptions = new FirefoxOptions(firefoxCapabilities)
    System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true")
    System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null")
    new FirefoxDriver(firefoxOptions)
  }

  lazy val chromeWebDriver = {
    System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    System.setProperty("browser", "chrome")
    val capabilities = DesiredCapabilities.chrome()
    new ChromeDriver(capabilities)
  }

  val driver: WebDriver = Properties.propOrElse("browser", "firefox") match {
    case "firefox"        => firefoxDriver
    case "chrome"         => chromeWebDriver
    case "remote-chrome"  => createRemoteChrome
    case "remote-firefox" => createRemoteFirefox
  }

  def createRemoteChrome: WebDriver = {
    new RemoteWebDriver(new URL(s"http://localhost:4444/wd/hub"), DesiredCapabilities.chrome())
  }

  def createRemoteFirefox: WebDriver = {
    new RemoteWebDriver(new URL(s"http://localhost:4444/wd/hub"), DesiredCapabilities.firefox())
  }

  def closeInstance(): Unit = driver.quit()
}
