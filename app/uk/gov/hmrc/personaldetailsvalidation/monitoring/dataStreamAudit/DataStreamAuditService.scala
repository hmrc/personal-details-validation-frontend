/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{MonitoringEvent, PdvFailedAttempt, PdvLockedOut}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataStreamAuditService @Inject()(auditConnector: AuditConnector) {

  val AUDIT_SOURCE = "personal-details-validation-frontend"

  def audit(event: MonitoringEvent)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[AuditResult] = {
    event match {
      case e: PdvFailedAttempt => sendPdvFailedAttemptEvent(e)
      case e: PdvLockedOut => sendPdvLockedOutEvent(e)
    }
  }

  def sendPdvFailedAttemptEvent(event: PdvFailedAttempt)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[AuditResult] = {
    val detail: JsObject = Json.obj(
      "failureCount" -> event.attempts,
      "maxAttempts" -> event.maxAttempts,
      "journeyVersion" -> event.journeyVersion,
      "credID" -> event.credID,
      "origin" -> event.origin,
      "outcome" -> "Failure"
    )
    val eventToSent: ExtendedDataEvent = {
      ExtendedDataEvent(
        auditSource = AUDIT_SOURCE,
        auditType = "PersonalDetailsValidationFailedAttempt",
        detail = Json.toJson(detail)
      )
    }
    sendExtendedEvent(eventToSent)
  }

  def sendPdvLockedOutEvent(event: PdvLockedOut)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[AuditResult] = {
    val detail: JsObject = if (event.journeyVersion.equals("reattempt PDV within 24 hours")) {
      Json.obj(
        "journeyVersion" -> "-",
        "failureReason" -> event.journeyVersion,
        "credID" -> event.credID,
        "origin" -> event.origin
      )
    } else {
      Json.obj(
        "journeyVersion" -> event.journeyVersion,
        "failureReason" -> "Previously locked",
        "credID" -> event.credID,
        "origin" -> event.origin
      )
    }

    val eventToSent: ExtendedDataEvent = {
      ExtendedDataEvent(
        auditSource = AUDIT_SOURCE,
        auditType = "PersonalDetailsValidationLocked",
        detail = Json.toJson(detail)
      )
    }
    sendExtendedEvent(eventToSent)
  }

  def sendExtendedEvent(event: ExtendedDataEvent)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[AuditResult] = {
    auditConnector.sendExtendedEvent(event) recover {
      case t: Throwable =>
        AuditResult.Failure(s"Failed sending audit message ${event.auditType}", Some(t))
    }
  }

}

