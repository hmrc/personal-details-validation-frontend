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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{BeginPDV, EventDispatcher}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PersonalDetailsValidationStartController @Inject()(journeyStart: JourneyStart,
                                                         eventDispatcher: EventDispatcher,
                                                         mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) {

  def start(completionUrl: CompletionUrl, origin: Option[String], failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(BeginPDV())
    journeyStart.findRedirect(completionUrl, origin, failureUrl)
  }
}
