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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder


trait ComponentSpecHelper extends AnyWordSpec with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with GuiceOneServerPerSuite
  with WiremockHelper {

  val config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "microservice.services.identity-verification.host" -> wiremockHost,
    "microservice.services.identity-verification.port" -> wiremockPort.toString,
    "microservice.services.personal-details-validation.host" -> wiremockHost,
    "microservice.services.personal-details-validation.port" -> wiremockPort.toString
  )

  def extraConfig: Map[String, String] = Map.empty

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config ++ extraConfig)
    .build()

  override def beforeAll(): Unit = {
    startWiremock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    resetWiremock()
    super.beforeEach()
  }

}
