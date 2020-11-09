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

package uk.gov.hmrc.personaldetailsvalidation.model

case class CompletionUrl private[CompletionUrl](value: String) extends StringValue

object CompletionUrl {

  def completionUrl(value: String): Either[IllegalArgumentException, CompletionUrl] = for {
    _ <- validateRelativeUrl(value)
    _ <- validateProtocolRelativeUrlSafe(value)
  } yield CompletionUrl(value)

  private def validateRelativeUrl(url: String) = validate(
    url.isLocalhost || url.startsWith("/"),
    s"$url is not a relative url"
  )

  private def validateProtocolRelativeUrlSafe(url: String) = validate(
    url.isLocalhost || !url.contains("//"),
    s"$url is not protocol relative url safe"
  )

  private implicit class UrlOps(url: String) {
    lazy val isLocalhost = url.startsWith("http://localhost")
  }

  private def validate(condition: => Boolean, errorMessage: => String): Either[IllegalArgumentException, Unit] =
    Either.cond(condition, (), new IllegalArgumentException(errorMessage))

}
