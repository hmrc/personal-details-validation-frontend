/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.personaldetails

import javax.inject.Inject

import play.api.mvc._
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyId
import uk.gov.hmrc.personaldetailsvalidation.personaldetails.verifiers.JourneyIdVerifier
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

class PersonalDetailsCollectionController @Inject()(private val page: PersonalDetailsPage,
                                                    private val journeyIdVerifier: JourneyIdVerifier)
  extends FrontendController {

  import journeyIdVerifier._

  def showPage(journeyId: JourneyId): Action[AnyContent] = forExisting(journeyId) async { implicit request =>
    Future.successful(Ok(page.render))
  }
}