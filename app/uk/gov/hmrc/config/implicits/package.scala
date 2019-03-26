/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.Duration

import cats.data.Validated._
import cats.data.ValidatedNel
import cats.implicits._
import play.api.Configuration
import uk.gov.hmrc.http.Host

import scala.collection.JavaConverters._

package object implicits {

  import ops._

  implicit def stringValueFinder(key: String)(configuration: Configuration): ValidatedNel[String, String] =
    configuration.getString(key).toValidated(key)

  implicit def stringValuesFinder(key: String)(configuration: Configuration): ValidatedNel[String, Seq[String]] =
    configuration.getStringList(key).map(_.asScala.toList).toValidated(key)

  implicit def intValueFinder(key: String)(configuration: Configuration): ValidatedNel[String, Int] =
    configuration.getInt(key).toValidated(key)


  implicit def hostFinder(key: String)(configuration: Configuration): ValidatedNel[String, Host] = {
    val servicesKey = "microservice.services"
    val defaultProtocol = configuration.load(s"$servicesKey.protocol", "http")
    val protocol = configuration.load(s"$servicesKey.$key.protocol", defaultProtocol)
    val validatedHost = configuration.loadValidated[String](s"$servicesKey.$key.host")
    val validatedPort = configuration.loadValidated[Int](s"$servicesKey.$key.port")

    (validatedHost, validatedPort) match {
      case (Valid(host), Valid(port)) => Host(s"$protocol://$host:$port").validNel
      case (invalid@Invalid(_), Valid(_)) => invalid
      case (Valid(_), invalid@Invalid(_)) => invalid
      case (Invalid(hostErrors), Invalid(portErrors)) => Invalid(hostErrors.concatNel(portErrors))
    }
  }

  implicit def durationFinder(key: String)(configuration: Configuration): ValidatedNel[String, Duration] =
    configuration.loadOptional[String](key).map(Duration.parse).toValidated(key)


  private implicit class ValueOps[V](maybeV: Option[V]) {
    def toValidated(key: String): ValidatedNel[String, V] = maybeV.toValidNel(s"$key not found")
  }

}