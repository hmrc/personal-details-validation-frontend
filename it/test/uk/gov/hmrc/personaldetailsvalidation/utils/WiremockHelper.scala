/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.personaldetailsvalidation.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.{Eventually, IntegrationPatience}

trait WiremockHelper extends Eventually with IntegrationPatience {

  val wiremockPort: Int = 11111
  val wiremockHost: String = "localhost"

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer: WireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

  def stubGet(url: String)(status: Int, optBody: Option[String] = None): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(createResponse(status, optBody))
    )

  def stubPost(url: String)(status: Int, optBody: Option[String] = None): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(createResponse(status, optBody))
    )

  def stubPatch(url: String)(status: Int, optBody: Option[String] = None): StubMapping =
    stubFor(patch(urlMatching(url))
      .willReturn(createResponse(status, optBody))
    )

  def stubPatchFault(uri: String)(fault: Fault): StubMapping =
    stubFor(patch(urlMatching(uri))
      .willReturn(aResponse().withFault(fault))
    )

  def verifyGet(uri: String): Unit = verify(getRequestedFor(urlEqualTo(uri)))

  def verifyPatch(uri: String, optBody: Option[String]): Unit = {

    val uriMapping = patchRequestedFor(urlEqualTo(uri))
    val patchRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }

    verify(patchRequest)

  }

  def verifyPost(uri: String, optBody: Option[String]): Unit = {

    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = createRequest(uriMapping, optBody)

    verify(postRequest)
  }

  private def createResponse(status: Int, optBody: Option[String]): ResponseDefinitionBuilder =
    optBody match {
      case Some(body) => aResponse().withStatus(status).withBody(body)
      case None => aResponse().withStatus(status)
    }

  private def createRequest(uriMapping: RequestPatternBuilder, optBody: Option[String]): RequestPatternBuilder = {
    optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
  }

}
