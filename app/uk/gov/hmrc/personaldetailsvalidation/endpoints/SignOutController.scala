/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, SignedOut}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignOutController @Inject()(cc: MessagesControllerComponents, eventDispatcher: EventDispatcher)
                                 (implicit appConfig: AppConfig, executionContext: ExecutionContext)
  extends FrontendController(cc) {

  def signOut(): Action[AnyContent] = Action.async { implicit request =>

    eventDispatcher.dispatchEvent(SignedOut)

    val ggRedirectParms = Map(
      "continue" -> Seq(s"${appConfig.logoutCallback}"),
      "origin"   -> Seq("pve")
    )

    Future.successful(Redirect(appConfig.ggLogoutUrl, ggRedirectParms))
  }

  def signedOut(): Action[AnyContent] = Action {
    appConfig.isLoggedInUser = Future.successful(false)
    Redirect(appConfig.logoutPage)
  }

}
