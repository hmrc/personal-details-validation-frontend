package uk.gov.hmrc.personaldetailsvalidation.support

import java.net.URL

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}
import org.openqa.selenium.remote.{DesiredCapabilities, RemoteWebDriver}

object SingletonDriver {
  import scala.util.Properties

  lazy val firefoxDriver = {
    val profile: FirefoxProfile = new FirefoxProfile
    profile.setPreference("javascript.enabled", true)
    profile.setAcceptUntrustedCertificates(true)
    new FirefoxDriver(profile)
  }

  lazy val chromeWebDriver = {
    System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")
    System.setProperty("browser", "chrome")
    val capabilities = DesiredCapabilities.chrome()
    new ChromeDriver(capabilities)
  }

  val driver: WebDriver = Properties.propOrElse("browser", "firefox") match {
    case "firefox"        => firefoxDriver
    case "chrome" => chromeWebDriver
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
