/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.config

import com.google.inject.Inject
import play.api.{Configuration, Environment}
import play.api.i18n.{DefaultMessagesApi, Langs, Messages}
import play.utils.Resources

class DwpMessagesApi @Inject() (environment: Environment, configuration: Configuration, langs: Langs)
  extends DefaultMessagesApi(environment, configuration, langs) {

  override protected def loadMessages(file: String): Map[String, String] = {
    import scala.collection.JavaConverters._

    // TODO: What do we do if this config entry is not here?
    val dwpMessagesPrefix = configuration.getString("dwp.messages")

    environment.classLoader.getResources(joinPaths(dwpMessagesPrefix, file)).asScala.toList
      .filterNot(url => Resources.isDirectory(environment.classLoader, url)).reverse
      .map { messageFile =>
        Messages.parse(Messages.UrlMessageSource(messageFile), messageFile.toString).fold(e => throw e, identity)
      }.foldLeft(Map.empty[String, String]) { _ ++ _ }
  }

  private def isDirectory(directory: String):String = directory.takeRight(1) match {
    case "/" => directory
    case _ => directory + "/"
  }

  private def joinPaths(first: Option[String], second: String): String = first match {
    case Some(parent) => new java.io.File(isDirectory(parent), second).getPath
    case None => second
  }
}
