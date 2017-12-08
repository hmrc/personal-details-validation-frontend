package uk.gov.hmrc.personaldetailsvalidationfrontend.specs

import org.scalatest.WordSpec
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.integration.framework.SingletonDriver
import uk.gov.hmrc.personaldetailsvalidationfrontend.support.{ImplicitWebDriverSugar, NavigationSugar}

trait BaseIntegrationSpec
  extends WordSpec with GuiceOneServerPerSuite with ScalaFutures
    with IntegrationPatience with ImplicitWebDriverSugar with NavigationSugar {

  protected def additionalConfiguration = Map.empty[String, Any]

  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  override lazy val port: Int = 9000

  protected val baseUrl = s"http://localhost:$port/personal-details-validation"

  protected def goTo(url: String) = super.goTo(s"$baseUrl$url")

  sys addShutdownHook {
    SingletonDriver.closeInstance()
  }

}


