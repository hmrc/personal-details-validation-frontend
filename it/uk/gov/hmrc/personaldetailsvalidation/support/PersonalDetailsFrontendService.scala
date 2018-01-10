package uk.gov.hmrc.personaldetailsvalidation.support

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait PersonalDetailsFrontendService extends GuiceOneServerPerSuite {
  self: TestSuite =>

  protected def additionalConfiguration = Map.empty[String, Any]

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(additionalConfiguration).build()

  override lazy val port: Int = 9000

}
