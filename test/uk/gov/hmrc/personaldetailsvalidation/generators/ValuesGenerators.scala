/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl.completionUrl
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, NonEmptyString, ValidationId}

object ValuesGenerators {

  import generators.Generators._

  implicit val uris: Gen[URI] = for {
    path <- Gen.nonEmptyListOf(strings(5, 10)).map(_.mkString("/"))
    paramCount <- positiveInts(3)
    keys <- Gen.listOfN(paramCount, strings(5, 10))
    values <- Gen.listOfN(paramCount, strings(5, 10))
    queryParams = keys.zip(values).map { case (key, value) => s"$key=$value"}.mkString("&")
  } yield new URI(s"/$path?$queryParams")

  implicit val completionUrls: Gen[CompletionUrl] = uris
    .map { uri =>
      completionUrl(uri.toString).fold(throw _, identity)
    }

  implicit val nonEmptyStringObjects: Gen[NonEmptyString] =
    nonEmptyStrings.map(NonEmptyString.apply)

  implicit val ninos: Gen[Nino] = {
    val ninoGenerator = new Generator()
    Gen.identifier.map(_ => ninoGenerator.nextNino)
  }

  implicit val validationIds: Gen[ValidationId] = Gen.uuid.map(aUUID => ValidationId(aUUID.toString))
}
