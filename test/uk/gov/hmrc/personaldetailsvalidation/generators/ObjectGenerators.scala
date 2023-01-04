/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalacheck.Gen
import support.Generators._
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators._
import uk.gov.hmrc.personaldetailsvalidation.model._

object ObjectGenerators {

  implicit val personalDetailsObjects: Gen[PersonalDetailsWithNino] = for {
    firstName <- nonEmptyStringObjects
    lastName <- nonEmptyStringObjects
    dateOfBirth <- legalLocalDates
    nino <- ninos
  } yield PersonalDetailsWithNino(firstName, lastName, nino, dateOfBirth)

  implicit val personalDetailsObjectsWithPostcode: Gen[PersonalDetailsWithPostcode] = for {
    firstName <- nonEmptyStringObjects
    lastName <- nonEmptyStringObjects
    dateOfBirth <- legalLocalDates
    postcode <- postCode
  } yield PersonalDetailsWithPostcode(firstName, lastName, postcode, dateOfBirth)

  implicit val personalDetailsValidationObjects: Gen[PersonalDetailsValidation] = for {
    boolean <- booleans
    validationId <- validationIds
  } yield if(boolean) SuccessfulPersonalDetailsValidation(validationId) else FailedPersonalDetailsValidation(validationId, "", 0)

  implicit val successfulPersonalDetailsValidationObjects: Gen[SuccessfulPersonalDetailsValidation] = for {
    validationId <- validationIds
  } yield SuccessfulPersonalDetailsValidation(validationId)

  implicit val failedPersonalDetailsValidationObjects: Gen[FailedPersonalDetailsValidation] = for {
    validationId <- validationIds
  } yield FailedPersonalDetailsValidation(validationId, "", 0)
}
