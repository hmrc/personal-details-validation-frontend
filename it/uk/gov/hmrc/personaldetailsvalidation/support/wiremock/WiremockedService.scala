package uk.gov.hmrc.personaldetailsvalidation.support.wiremock

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import uk.gov.hmrc.personaldetailsvalidation.support.PersonalDetailsFrontendService

trait WiremockedService
  extends WiremockedServiceConfig
    with WiremockServerInstance
    with WiremockedServiceBuilder
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  suite: Suite with PersonalDetailsFrontendService =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }
}
