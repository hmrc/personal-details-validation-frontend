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

package uk.gov.hmrc.personaldetailsvalidation.connectors


import java.util.UUID
import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import play.api.test.Helpers._
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.personaldetailsvalidation.utils.{ComponentSpecHelper, LogCapturing}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IdentityVerificationConnectorISpec extends ComponentSpecHelper with LogCapturing {

  "IdentityVerificationConnector" should {

    "update the journey status to timeout in identity verification" in new Setup {

      stubPatch(backendUrl)(OK)

      withCaptureOfLoggingFrom(identityVerificationConnector.testLogger, Level.WARN) {logEvents =>

        val result: Any = identityVerificationConnector.updateJourney(redirectingUrl, timeout)

        eventually {

          result.asInstanceOf[Future[HttpResponse]].value match {
            case Some(tryResult) => tryResult match {
              case Success(response) => response.status shouldBe OK
              case Failure(_) => fail("Future failed")
            }
            case None => fail("No Try result returned")
          }

          logEvents.count(_.getLevel == Level.WARN) shouldBe 0

        }

        val expectedRequestBody: String = s"""{"journeyStatus":"$timeout"}"""

        verifyPatch(backendUrl, Some(expectedRequestBody))
      }

    }

    "update the journey status to user abort in identity verification" in new Setup {

      stubPatch(backendUrl)(OK)

      withCaptureOfLoggingFrom(identityVerificationConnector.testLogger, Level.WARN) {logEvents =>

        val result: Any = identityVerificationConnector.updateJourney(redirectingUrl, userAborted)

        eventually {

          result.asInstanceOf[Future[HttpResponse]].value match {
            case Some(tryResult) => tryResult match {
              case Success(response) => response.status shouldBe OK
              case Failure(_) => fail("Future failed")
            }
            case None => fail("No Try result returned")
          }

          logEvents.count(_.getLevel == Level.WARN) shouldBe 0

        }

        val expectedRequestBody: String = s"""{"journeyStatus":"$userAborted"}"""

        verifyPatch(backendUrl, Some(expectedRequestBody))
      }

    }

    "manage a response status of not found" in new Setup {

      stubPatch(backendUrl)(NOT_FOUND)

      withCaptureOfLoggingFrom(identityVerificationConnector.testLogger, Level.WARN) { logEvents =>

        val result: Any = identityVerificationConnector.updateJourney(redirectingUrl, success)

        eventually {

          result.asInstanceOf[Future[HttpResponse]].value match {
            case Some(tryResult) => tryResult match {
              case Success(response) => response.status shouldBe NOT_FOUND
              case Failure(_) => fail("Future failed")
            }
            case None => fail("No Try result returned")
          }

          logEvents.count(_.getLevel == Level.WARN) shouldBe 0
        }

        val expectedRequestBody: String = s"""{"journeyStatus":"$success"}"""

        verifyPatch(backendUrl, Some(expectedRequestBody))
      }

    }

    "log a fault" in new Setup {

      stubPatchFault(backendUrl)(Fault.MALFORMED_RESPONSE_CHUNK)

      withCaptureOfLoggingFrom(identityVerificationConnector.testLogger, Level.WARN) { logEvents =>

        identityVerificationConnector.updateJourney(redirectingUrl, success)

        eventually {

          logEvents.count(_.getLevel == Level.WARN) shouldBe 1

          logEvents.filter(_.getLevel == Level.WARN).loneElement.getMessage shouldBe s"IV returns error Remotely closed, update IV journey might failed for $testJourneyId"
        }

        val expectedRequestBody: String = s"""{"journeyStatus":"$success"}"""

        verifyPatch(backendUrl, Some(expectedRequestBody))
      }
    }

  }

  private trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = ExecutionContext.global

    val mdtpUrl: String = "/mdtp/personal-details-validation-complete"
    val testJourneyId: String = UUID.randomUUID().toString
    val timeout: String = "Timeout"
    val userAborted: String = "UserAborted"
    val success: String = "Success"

    val redirectingUrl: String = s"$mdtpUrl/$testJourneyId"
    val backendUrl: String = s"/identity-verification/journey/$testJourneyId"

    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

    val identityVerificationConnector = new IdentityVerificationConnector(appConfig, httpClient){
      val testLogger = logger
    }
  }

}
