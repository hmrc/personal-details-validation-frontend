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

package uk.gov.hmrc.config

import java.net.URL

import com.google.inject.Inject
import play.api.http.HttpConfiguration
import play.api.i18n.{DefaultMessagesApiProvider, Langs, Messages}
import play.api.{Configuration, Environment, Logger}
import play.utils.Resources


class DwpMessagesApiProvider @Inject()(environment: Environment,
                                       configuration: Configuration,
                                       langs: Langs,
                                       httpConfiguration: HttpConfiguration)
  extends DefaultMessagesApiProvider(environment, configuration, langs, httpConfiguration) {

  override protected def loadMessages(file: String): Map[String, String] = {
    import scala.collection.JavaConverters._

    val dwpMessagesPrefix = configuration.getOptional[String]("dwp.messages")

    environment.classLoader.getResources(joinPaths(dwpMessagesPrefix, file)).asScala.toList match {
      case r if r.isEmpty => {
        Logger.warn(s"DWP messages directory in 'dwp.messages' : $dwpMessagesPrefix is not valid")
        getMessages(environment.classLoader.getResources(file).asScala.toList)
      }
      case r => getMessages(r)
    }
  } ++ super.loadMessages("messages") ++ super.loadMessages("messages.default")

  private def getMessages(resources: List[URL]) ={
    resources.filterNot(url => Resources.isDirectory(environment.classLoader, url)).reverse
      .map { messageFile =>
        Messages.parse(Messages.UrlMessageSource(messageFile), messageFile.toString).fold(e => throw e, identity)
      }.foldLeft(Map.empty[String, String]) {
      _ ++ _
    }
  }

  private def isDirectory(directory: String):String = directory.takeRight(1) match {
    case "/" => directory
    case _ => directory + "/"
  }

  override protected def joinPaths(first: Option[String], second: String): String = first match {
    case Some(parent) => new java.io.File(isDirectory(parent), second).getPath
    case None =>
      Logger.warn(s"DWP messages file location property 'dwp.messages' not set in app config")
      second
  }
}
