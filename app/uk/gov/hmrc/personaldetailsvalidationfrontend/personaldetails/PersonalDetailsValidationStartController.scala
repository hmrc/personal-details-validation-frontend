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

package uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails

import javax.inject.{Inject, Singleton}

import play.api.mvc.Action
import uk.gov.hmrc.personaldetailsvalidationfrontend.model.{JourneyId, RelativeUrl}
import uk.gov.hmrc.personaldetailsvalidationfrontend.uuid.UUIDProvider
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class PersonalDetailsValidationStartController @Inject()(private implicit val uuidProvider: UUIDProvider)
  extends FrontendController {

  def start(completionUrl: RelativeUrl) = Action {
    Redirect(routes.PersonalDetailsCollectionController.showPage(JourneyId()))
  }
}