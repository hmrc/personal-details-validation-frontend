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

package uk.gov.hmrc.formmappings

import java.time.temporal.ChronoField._
import java.time.{DateTimeException, LocalDate}

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import play.api.data.validation.{Constraint, ValidationError}
import play.api.data.{FormError, Mapping, ObjectMapping}

import scala.util.{Failure, Success, Try}

case class LocalDateMapping private[formmappings](key: String = "",
                                                  constraints: Seq[Constraint[LocalDate]] = Nil)
                                                 (errorKeyPrefix: => String)
  extends Mapping[LocalDate]
    with ObjectMapping {

  private lazy val binder = LocalDateMapping.LocalDateBinder(key, constraints)(errorKeyPrefix)

  import binder._

  val mappings: Seq[Mapping[_]] = Seq(this)

  def bind(formData: Map[String, String]): Seq[FormError] Either LocalDate = binder.bind(formData)

  def unbind(date: LocalDate): Map[String, String] = Map(
    "year".prependWithKey -> date.getYear.toString,
    "month".prependWithKey -> date.getMonthValue.toString,
    "day".prependWithKey -> date.getDayOfMonth.toString
  )

  def unbindAndValidate(date: LocalDate): (Map[String, String], Seq[FormError]) =
    unbind(date) -> collectErrors(date)

  def withPrefix(prefix: String): Mapping[LocalDate] =
    addPrefix(prefix)
      .map(newKey => LocalDateMapping(newKey, constraints)(errorKeyPrefix))
      .getOrElse(this)

  def verifying(addConstraints: Constraint[LocalDate]*): Mapping[LocalDate] =
    LocalDateMapping(key, constraints ++ addConstraints.toSeq)(errorKeyPrefix)
}

private object LocalDateMapping {

  private case class LocalDateBinder(key: String,
                                     constraints: Seq[Constraint[LocalDate]])
                                    (errorKeyPrefix: => String) {

    def bind(formData: Map[String, String]): Either[Seq[FormError], LocalDate] = Seq(
      "year".prependWithKey.findValueIn(formData).parseToInt.validateUsing(YEAR.checkValidValue),
      "month".prependWithKey.findValueIn(formData).parseToInt.validateUsing(MONTH_OF_YEAR.checkValidValue),
      "day".prependWithKey.findValueIn(formData).parseToInt.validateUsing(DAY_OF_MONTH.checkValidValue)
    ).toValidatedDate
      .leftMap(toFormErrors)
      .checkConstraints
      .toEither

    implicit class PartNameOps(partName: String) {

      lazy val prependWithKey: String = key match {
        case "" => partName
        case nonEmptyKey => s"$nonEmptyKey.$partName"
      }

      private[LocalDateBinder] def findValueIn(formData: Map[String, String]): ValidatedNel[String, (String, String)] =
        Validated.fromOption(
          formData.get(partName).flatMap(blankToNone).map(part => partName -> part),
          NonEmptyList.of(s"$errorKeyPrefix.$partName.required")
        )

      private val blankToNone: String => Option[String] =
        _.trim match {
          case "" => None
          case nonEmpty => Some(nonEmpty)
        }
    }

    private implicit class ValidatedStringPartOps(validatedPart: ValidatedNel[String, (String, String)]) {

      lazy val parseToInt: ValidatedNel[String, (String, Int)] = validatedPart match {
        case invalid@Validated.Invalid(error) =>
          invalid
        case Validated.Valid((partName, stringValue)) =>
          Try(stringValue.toInt) match {
            case Success(partAsInt) => Validated.validNel(partName -> partAsInt)
            case Failure(_) => Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
          }
      }
    }

    private implicit class ValidatedIntPartOps(validatedPart: ValidatedNel[String, (String, Int)]) {

      def validateUsing(validate: Long => Long): ValidatedNel[String, Int] = validatedPart match {
        case invalid@Validated.Invalid(error) =>
          invalid
        case Validated.Valid((partName, intValue)) =>
          Try(validate(intValue)) match {
            case Success(validatedValue) => Validated.validNel(validatedValue.intValue())
            case Failure(_) => Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
          }
      }
    }

    private implicit class ValidatedPartsOps(validatedParts: Seq[ValidatedNel[String, Int]]) {

      lazy val toValidatedDate: ValidatedNel[String, LocalDate] = validatedParts.foldLeft(Validated.validNel[String, Seq[Int]](Nil)) {
        case (Validated.Valid(parts), Validated.Valid(part)) => Validated.validNel(parts :+ part)
        case (Validated.Valid(_), invalid@Validated.Invalid(_)) => invalid
        case (invalidParts@Validated.Invalid(_), Validated.Invalid(errors)) => invalidParts.leftMap[NonEmptyList[String]](_ concatNel errors)
        case (invalidParts@Validated.Invalid(_), Validated.Valid(_)) => invalidParts
      } match {
        case invalid@Validated.Invalid(_) => invalid
        case Validated.Valid(parts) => toDate(parts)
      }

      private def toDate: Seq[Int] => ValidatedNel[String, LocalDate] = {
        case year :: month :: day :: Nil =>
          Validated.catchOnly[DateTimeException](LocalDate.of(year, month, day))
            .leftMap(_ => NonEmptyList.of(dateFieldError))
        case _ => Validated.invalidNel(dateFieldError)
      }

      private lazy val dateFieldError = key match {
        case "" => s"$errorKeyPrefix.invalid"
        case nonEmptyKey => s"$errorKeyPrefix.$nonEmptyKey.invalid"
      }
    }

    private def toFormErrors(errorKeys: NonEmptyList[String]): List[FormError] =
      errorKeys.toList.map(FormError(key, _))

    private implicit class ValidatedDateOps(validatedDate: Validated[Seq[FormError], LocalDate]) {

      import play.api.data.validation.{Invalid, Valid}

      lazy val checkConstraints: Validated[Seq[FormError], LocalDate] = validatedDate match {
        case invalid@Validated.Invalid(_) => invalid
        case Validated.Valid(date) =>
          constraints
            .map(constraint => constraint(date))
            .foldLeft(Seq.empty[FormError]) {
              case (formErrors, Valid) => formErrors
              case (formErrors, Invalid(validationErrors)) => combineErrors(formErrors, validationErrors)
            }.toValidated(date)
      }

      private def combineErrors(formErrors: Seq[FormError], validationErrors: Seq[ValidationError]) =
        formErrors ++: validationErrors.map {
          error => FormError(key, error.message, error.args)
        }

      private implicit class FormErrorsOps(formErrors: Seq[FormError]) {
        def toValidated(date: LocalDate) = formErrors match {
          case Nil => Validated.valid(date)
          case errors => Validated.invalid(errors)
        }
      }
    }
  }
}
