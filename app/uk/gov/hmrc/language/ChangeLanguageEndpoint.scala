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

package uk.gov.hmrc.language

import javax.inject.{Inject, Singleton}
import play.api.i18n.Lang
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}
import uk.gov.hmrc.views.ViewConfig

@Singleton
class ChangeLanguageEndpoint  @Inject()(viewConfig: ViewConfig,
                                        languageUtils: LanguageUtils,
                                        cc: ControllerComponents)
  extends LanguageController(languageUtils, cc) {

  override val languageMap: Map[String, Lang] = viewConfig.languageMap

  override protected def fallbackURL: String =
    throw new RuntimeException("No Referrer found in request header - cannot redirect")

}
