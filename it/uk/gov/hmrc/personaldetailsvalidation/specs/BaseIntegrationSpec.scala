package uk.gov.hmrc.personaldetailsvalidation.specs

import org.scalatest.{FeatureSpec, GivenWhenThen}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.personaldetailsvalidation.support.{ImplicitWebDriverSugar, NavigationSugar, SingletonDriver}

trait BaseIntegrationSpec
  extends FeatureSpec with GivenWhenThen
    with GuiceOneServerPerSuite
    with ScalaFutures with IntegrationPatience
    with ImplicitWebDriverSugar with NavigationSugar {

  protected def additionalConfiguration = Map.empty[String, Any]

  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  override lazy val port: Int = 9000

  protected val baseUrl = s"http://localhost:$port/personal-details-validation"

  protected def goTo(url: String): Unit = super.goTo(s"$baseUrl$url")

  sys addShutdownHook {
    SingletonDriver.closeInstance()
  }
}
