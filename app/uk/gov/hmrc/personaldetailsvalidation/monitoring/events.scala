/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.monitoring

sealed trait MonitoringEvent

//Timeout events
case class TimeoutContinue() extends MonitoringEvent
case class TimedOut() extends MonitoringEvent
case class SignedOut() extends MonitoringEvent
case class UnderNinoAge() extends MonitoringEvent
case class PdvFailedAttempt(attempts: Int, maxAttempts: Int, journeyVersion: String, credID: String, origin: String) extends MonitoringEvent
case class PdvLockedOut(failureReason: String, credID: String, origin: String) extends MonitoringEvent

