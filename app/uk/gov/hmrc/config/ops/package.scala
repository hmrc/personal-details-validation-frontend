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

import cats.data.{NonEmptyList, ValidatedNel}
import play.api.Configuration

package object ops {

  implicit class ConfigurationOps(configuration: Configuration) {

    private type ConfigKey = String
    private type ConfigFinder[Value] = ConfigKey => Configuration => ValidatedNel[String, Value]

    def loadMandatory[A](key: String)
                        (implicit finder: ConfigFinder[A]): A =
      loadValidated(key)(finder).fold(throwRuntimeException, identity)

    def load[A](key: String, default: => A)
               (implicit finder: ConfigFinder[A]): A =
      loadValidated(key)(finder).fold(_ => default, identity)

    def loadOptional[A](key: String)
                       (implicit finder: ConfigFinder[A]): Option[A] =
      loadValidated(key)(finder).toOption

    private [config] def loadValidated[A](key: String)
                                (implicit finder: ConfigFinder[A]): ValidatedNel[String, A] =
      finder(key)(configuration)

    private def throwRuntimeException(errors: NonEmptyList[String]) = throw new RuntimeException(errors.toList.mkString(", "))

  }


}
