/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.monitoring.dataStreamAudit

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.JsObject
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{PdvFailedAttempt, PdvLockedOut}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataStreamAuditServiceSpec extends UnitSpec with MockFactory {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockAuditConnector = mock[AuditConnector]
  private val service            = new DataStreamAuditService(mockAuditConnector)

  "audit" should {

    "delegate to sendPdvFailedAttemptEvent for a PdvFailedAttempt event" in {
      val event = PdvFailedAttempt(attempts = 1, maxAttempts = 3, journeyVersion = "NINO", credID = "cred-1", origin = "ma")

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(AuditResult.Success))

      val result = await(service.audit(event))
      result shouldBe AuditResult.Success
    }

    "delegate to sendPdvLockedOutEvent for a PdvLockedOut event" in {
      val event = PdvLockedOut(journeyVersion = "NINO", credID = "cred-2", origin = "bta-sa")

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(AuditResult.Success))

      val result = await(service.audit(event))
      result shouldBe AuditResult.Success
    }
  }

  "sendPdvFailedAttemptEvent" should {

    "send an ExtendedDataEvent with the correct audit type and detail" in {
      val event = PdvFailedAttempt(attempts = 2, maxAttempts = 3, journeyVersion = "Postcode", credID = "cred-3", origin = "pta")

      var capturedEvent: ExtendedDataEvent = null

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .onCall { (e: ExtendedDataEvent, _: HeaderCarrier, _: scala.concurrent.ExecutionContext) =>
          capturedEvent = e
          Future.successful(AuditResult.Success)
        }

      await(service.sendPdvFailedAttemptEvent(event))

      capturedEvent.auditSource shouldBe "personal-details-validation-frontend"
      capturedEvent.auditType   shouldBe "PersonalDetailsValidationFailedAttempt"

      val detail = capturedEvent.detail.as[JsObject]
      (detail \ "failureCount").as[Int]       shouldBe 2
      (detail \ "maxAttempts").as[Int]        shouldBe 3
      (detail \ "journeyVersion").as[String]  shouldBe "Postcode"
      (detail \ "credID").as[String]          shouldBe "cred-3"
      (detail \ "origin").as[String]          shouldBe "pta"
      (detail \ "outcome").as[String]         shouldBe "Failure"
    }

    "return a Failure AuditResult when the connector throws" in {
      val event = PdvFailedAttempt(1, 3, "NINO", "cred-x", "ma")

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .returning(Future.failed(new RuntimeException("audit failed")))

      val result = await(service.sendPdvFailedAttemptEvent(event))

      result shouldBe a[AuditResult.Failure]
    }
  }

  "sendPdvLockedOutEvent" should {

    "send an ExtendedDataEvent with the correct audit type for a standard locked-out" in {
      val event = PdvLockedOut(journeyVersion = "NINO", credID = "cred-4", origin = "dwp-iv")

      var capturedEvent: ExtendedDataEvent = null

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .onCall { (e: ExtendedDataEvent, _: HeaderCarrier, _: scala.concurrent.ExecutionContext) =>
          capturedEvent = e
          Future.successful(AuditResult.Success)
        }

      await(service.sendPdvLockedOutEvent(event))

      capturedEvent.auditSource shouldBe "personal-details-validation-frontend"
      capturedEvent.auditType   shouldBe "PersonalDetailsValidationLocked"

      val detail = capturedEvent.detail.as[JsObject]
      (detail \ "journeyVersion").as[String]  shouldBe "NINO"
      (detail \ "failureReason").as[String]   shouldBe "Previously locked"
      (detail \ "credID").as[String]          shouldBe "cred-4"
      (detail \ "origin").as[String]          shouldBe "dwp-iv"
    }

    "use the reattempt-within-24-hours branch when journeyVersion matches" in {
      val event = PdvLockedOut(journeyVersion = "reattempt PDV within 24 hours", credID = "cred-5", origin = "bta")

      var capturedEvent: ExtendedDataEvent = null

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .onCall { (e: ExtendedDataEvent, _: HeaderCarrier, _: scala.concurrent.ExecutionContext) =>
          capturedEvent = e
          Future.successful(AuditResult.Success)
        }

      await(service.sendPdvLockedOutEvent(event))

      val detail = capturedEvent.detail.as[JsObject]
      (detail \ "journeyVersion").as[String]  shouldBe "-"
      (detail \ "failureReason").as[String]   shouldBe "reattempt PDV within 24 hours"
    }

    "return a Failure AuditResult when the connector throws" in {
      val event = PdvLockedOut("NINO", "cred-y", "ma")

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .returning(Future.failed(new RuntimeException("locked-out audit failed")))

      val result = await(service.sendPdvLockedOutEvent(event))

      result shouldBe a[AuditResult.Failure]
    }
  }

  "sendExtendedEvent" should {

    "return Success when the connector succeeds" in {
      val event = ExtendedDataEvent("source", "type")

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(AuditResult.Success))

      await(service.sendExtendedEvent(event)) shouldBe AuditResult.Success
    }

    "recover and return Failure when the connector throws" in {
      val event = ExtendedDataEvent("source", "type")

      (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(using _: HeaderCarrier, _: scala.concurrent.ExecutionContext))
        .expects(*, *, *)
        .returning(Future.failed(new RuntimeException("boom")))

      val result = await(service.sendExtendedEvent(event))

      result shouldBe a[AuditResult.Failure]
      result.asInstanceOf[AuditResult.Failure].msg should include("type")
    }
  }
}
