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

package uk.gov.hmrc.personaldetailsvalidation.generators

import java.net.URI

import org.scalacheck.Gen
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl.completionUrl

object ValuesGenerators {

  import generators.Generators._

  implicit val completionUrls: Gen[CompletionUrl] = Gen.nonEmptyListOf(strings(10))
    .map(_.mkString("/"))
    .map { path =>
      completionUrl(s"/$path").fold(throw _, identity)
    }

  implicit val uris: Gen[URI] = Gen.nonEmptyListOf(strings(10))
    .map(_.mkString("/"))
    .map(path => new URI(s"/$path"))

  implicit val ninos: Gen[Nino] = {
    val ninoGenerator = new Generator()
    Gen.identifier.map(_ => ninoGenerator.nextNino)
  }
}
