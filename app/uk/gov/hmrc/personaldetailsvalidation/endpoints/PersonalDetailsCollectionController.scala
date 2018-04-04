/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import play.api.mvc._
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

class PersonalDetailsCollectionController @Inject()(page: PersonalDetailsPage,
                                                    personalDetailsSubmission: FuturedPersonalDetailsSubmission)
  extends FrontendController {

  def showPage(implicit completionUrl: CompletionUrl, postcodeVersion: Boolean): Action[AnyContent] = Action { implicit request =>
    println("SHOW PAGEEEEEEEE " + completionUrl)
    Ok(page.render(postcodeVersion))
  }

  def submit(completionUrl: CompletionUrl, postcodeVersion: Boolean): Action[AnyContent] = Action.async { implicit request =>
    println("SUBMIT PAGEEEEEEEE " + completionUrl)
    personalDetailsSubmission.submit(completionUrl, postcodeVersion)
  }
}