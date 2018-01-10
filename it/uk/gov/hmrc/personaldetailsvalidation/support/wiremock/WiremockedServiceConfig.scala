package uk.gov.hmrc.personaldetailsvalidation.support.wiremock

import cats.implicits._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import uk.gov.hmrc.personaldetailsvalidation.support.PersonalDetailsFrontendService

trait WiremockedServiceBuilder {

  protected var wiremockedServiceName: Option[String] = None
  protected var wiremockedServiceHost: Option[String] = None
  protected var wiremockedServicePort: Option[Int] = None

  protected def wiremock(serviceName: String) = new {
    def on(host: String, port: Int): Unit = {
      wiremockedServiceName = Some(serviceName)
      wiremockedServiceHost = Some(host)
      wiremockedServicePort = Some(port)
    }
  }
}

trait WiremockedServiceConfig {

  self: WiremockedServiceBuilder with PersonalDetailsFrontendService =>

  override protected lazy val additionalConfiguration: Map[String, Any] =
    (wiremockedServiceName, wiremockedServiceHost, wiremockedServicePort).mapN {
      (serviceName, host, port) =>
        Map(
          s"microservice.services.$serviceName.host" -> host,
          s"microservice.services.$serviceName.port" -> port
        )
    }.getOrElse(Map.empty)
}

trait WiremockServerInstance {

  self: WiremockedServiceBuilder =>

  private lazy val maybeServer = (wiremockedServiceName, wiremockedServiceHost, wiremockedServicePort).mapN {
    (_, _, port) =>
      new WireMockServer(wireMockConfig().port(port))
  }

  protected def startWiremock(): Unit = (maybeServer, wiremockedServiceHost, wiremockedServicePort).mapN {
    (server, host, port) =>
      server.start()
      WireMock.configureFor(host, port)
  }

  protected def stopWiremock(): Unit = maybeServer foreach (_.stop())

  protected def resetWiremock(): Unit = maybeServer foreach {
    _ => WireMock.reset()
  }
}
