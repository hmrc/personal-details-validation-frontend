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

package uk.gov.hmrc.language

import com.google.inject.ImplementedBy
import play.api.Configuration
import play.api.i18n.{Lang, MessagesApi}
import uk.gov.hmrc.views.ViewConfig
import uk.gov.hmrc.config.ops._
import uk.gov.hmrc.config.implicits._

@ImplementedBy(classOf[ViewConfig])
trait LanguagesConfig {

  protected def configuration: Configuration

  protected def messagesApi: MessagesApi

  lazy val languagesMap: Map[String, Lang] =
    configuration.load[Seq[String]]("play.i18n.langs", default = Nil)
      .map(verifyMessagesExists)
      .map(toLangNameAndLangTuples)
      .toMap

  private def toLangNameAndLangTuples(code: String): (String, Lang) =
    configuration.loadMandatory[String](s"play.i18n.descriptions.$code") -> Lang(code)

  private def verifyMessagesExists(code: String): String = {
    val validatedCode = if (code == "en") "default" else code
    messagesApi.messages.keySet.find(_ == validatedCode) match {
      case Some(_) => code
      case None => throw new RuntimeException(s"No messages.$code defined")
    }
  }
}
