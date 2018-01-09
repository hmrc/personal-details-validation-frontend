package uk.gov.hmrc.personaldetailsvalidation.support

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FeatureSpec, GivenWhenThen}

trait BaseIntegrationSpec
  extends FeatureSpec with GivenWhenThen
    with PersonalDetailsFrontendService
    with ScalaFutures with IntegrationPatience
    with ImplicitWebDriverSugar with NavigationSugar {

  protected val baseUrl = s"http://localhost:$port/personal-details-validation"

  protected def goTo(url: String): Unit = super.goTo(s"$baseUrl$url")

  sys addShutdownHook {
    SingletonDriver.closeInstance()
  }
}
