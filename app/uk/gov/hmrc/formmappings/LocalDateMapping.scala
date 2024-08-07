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

package uk.gov.hmrc.formmappings

import java.time.temporal.ChronoField._
import java.time.temporal.{ChronoField, ChronoUnit}
import java.time.{DateTimeException, LocalDate}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import play.api.data.validation.{Constraint, ValidationError}
import play.api.data.{FormError, Mapping, ObjectMapping}
import uk.gov.hmrc.personaldetailsvalidation.model.DateErrorMessage

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

  import cats.implicits._

  private case class LocalDateBinder(key: String,
                                     constraints: Seq[Constraint[LocalDate]])
                                    (errorKeyPrefix: => String) {

    private val abbrMonths = Map(
      "JAN" -> 1, "FEB" -> 2, "MAR" -> 3, "APR" -> 4, "MAY" -> 5, "JUN" -> 6,
      "JUL" -> 7, "AUG" -> 8, "SEP" -> 9, "OCT" -> 10, "NOV" -> 11, "DEC" -> 12
    )

    private val months = Map(
      "JANUARY" -> 1, "FEBRUARY" -> 2, "MARCH" -> 3, "APRIL" -> 4, "MAY" -> 5, "JUNE" -> 6,
      "JULY" -> 7, "AUGUST" -> 8, "SEPTEMBER" -> 9, "OCTOBER" -> 10, "NOVEMBER" -> 11, "DECEMBER" -> 12
    )

    // works on each element of the map
    private def removeSpacesFromValues(pair: (String, String)): (String, String) = {
      val key = pair._1
      val value = pair._2
      (key, value.replaceAll(" ", ""))
    }

    def bind(formData: Map[String, String]): Either[Seq[FormError], LocalDate] = {
      val cleanedMap: Map[String, String] = formData.map(removeSpacesFromValues)
      Seq(
        "year".prependWithKey.findValueIn(cleanedMap).parseToInt.validateUsing(fourDigitsValidator),
        "month".prependWithKey.findValueIn(cleanedMap).validateMonthUsing(monthValidator),
        "day".prependWithKey.findValueIn(cleanedMap).parseToInt.validateUsing(DAY_OF_MONTH)
      ).toValidatedDate
        .leftMap(toFormErrors)
        .checkConstraints
        .toEither
    }

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

      lazy val parseToInt: ValidatedNel[String, (String, Int)] = validatedPart flatMap {
        case (partName, stringValue) =>
          Try(stringValue.toInt) match {
            case Success(partAsInt) => Validated.validNel(partName -> partAsInt)
            case Failure(_) => Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
          }
      }

      def validateMonthUsing(validate: ((String, String)) => ValidatedNel[String, Int]): ValidatedNel[String, Int] =
        validatedPart flatMap validate
    }

    private implicit class ValidatedIntPartOps(validatedPart: ValidatedNel[String, (String, Int)]) {

      def validateUsing(validate: ((String, Int)) => ValidatedNel[String, Int]): ValidatedNel[String, Int] =
        validatedPart flatMap validate
    }

    private implicit def asTupleToValidated(field: ChronoField): ((String, Int)) => ValidatedNel[String, Int] = {
      case (partName: String, partValue: Int) => Try(field.checkValidValue(partValue)) match {
        case Success(validatedValue) => Validated.validNel(validatedValue.intValue())
        case Failure(_) => Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
      }
    }

    private val fourDigitsValidator: ((String, Int)) => ValidatedNel[String, Int] = {
      case (partName, year) =>
        if (year > 999 && year < 9999) Validated.validNel(year)
        else Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
    }

    private val monthValidator: ((String, String)) => ValidatedNel[String, Int] = {
      case (partName, month) =>
        Try(month.toInt) match {
          case Success(month) =>
            if (month >= 1 && month <= 12) Validated.validNel(month)
            else Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
          case Failure(_) =>
            if (months contains month.toUpperCase) Validated.validNel(months.getOrElse(month.toUpperCase, 0))
            else if (abbrMonths contains month.toUpperCase) Validated.validNel(abbrMonths.getOrElse(month.toUpperCase, 0))
            else Validated.invalidNel(s"$errorKeyPrefix.$partName.invalid")
        }
    }

    private implicit class ValidatedPartsOps(seqOfValidatedParts: Seq[ValidatedNel[String, Int]]) {

      lazy val toValidatedDate: ValidatedNel[String, LocalDate] =
        seqOfValidatedParts
          .toValidatedSeqOfParts
          .leftMap(toSingleErrorIfAllPartsMissing)
          .flatMap(toDate)
          .flatMap(checkAge)

      private[ValidatedPartsOps] lazy val toValidatedSeqOfParts: ValidatedNel[String, Seq[Int]] =
        seqOfValidatedParts.foldLeft(Validated.validNel[String, List[Int]](Nil)) {
          case (combinedParts, validatedPart) => combinedParts combine validatedPart.map(List(_))
        }

      private val numberOfDateParts = 3

      private def toSingleErrorIfAllPartsMissing(errors: NonEmptyList[String]): NonEmptyList[String] =
        errors.filter(_ endsWith ".required").size match {
          case `numberOfDateParts` => NonEmptyList.of(dateFieldError(suffixed = "required"))
          case _ => errors
        }

      private val toDate: Seq[Int] => ValidatedNel[String, LocalDate] = {
        case year +: month +: day +: Nil =>
          Validated.catchOnly[DateTimeException](LocalDate.of(year, month, day))
            .leftMap(_ => NonEmptyList.of(dateFieldError(suffixed = "invalid")))
        case _ => Validated.invalidNel(dateFieldError(suffixed = "invalid"))
      }

      private def checkAge(birthDate: LocalDate): ValidatedNel[String, LocalDate] = {
        val MINIMUM_AGE_REQUIRED_IN_MONTHS: Int = 189 //15yrs and 9 months
        val currentDate = LocalDate.now()
        if (currentDate.isBefore(birthDate)) Validated.invalidNel(dateFieldError(suffixed = "mustInPast"))
        else if (ChronoUnit.MONTHS.between(birthDate, currentDate) < MINIMUM_AGE_REQUIRED_IN_MONTHS) Validated.invalidNel(dateFieldError(suffixed = "tooYoung"))
        else Validated.valid(birthDate)
      }

      private def dateFieldError(suffixed: String) = key match {
        case "" => s"$errorKeyPrefix.$suffixed"
        case nonEmptyKey => s"$errorKeyPrefix.$nonEmptyKey.$suffixed"
      }
    }

    private def toFormErrors(errorKeys: NonEmptyList[String]): List[FormError] = {
      val link : String = DateErrorMessage.getDateLink(key, errorKeys.toList)
      DateErrorMessage.getErrorSummaryMessage(key, errorKeys.toList).map(FormError(link, _))
    }

    private implicit class ValidatedDateOps(validatedDate: Validated[Seq[FormError], LocalDate]) {

      import play.api.data.validation.{Invalid, Valid}

      lazy val checkConstraints: Validated[Seq[FormError], LocalDate] = validatedDate flatMap { date =>
        constraints
          .map(constraint => constraint(date))
          .foldLeft(Seq.empty[FormError]) {
            case (formErrors, Valid) => formErrors
            case (formErrors, Invalid(validationErrors)) => combineErrors(formErrors, validationErrors)
          }.toValidated(date)
      }

      private def combineErrors(formErrors: Seq[FormError], validationErrors: Seq[ValidationError]) = {
        formErrors ++: validationErrors.map {
          error => FormError(key, error.message, error.args)
        }
      }

      private implicit class FormErrorsOps(formErrors: Seq[FormError]) {
        def toValidated(date: LocalDate): Validated[Seq[FormError], LocalDate] = formErrors match {
          case Nil => Validated.valid(date)
          case errors => Validated.invalid(errors)
        }
      }
    }

    private implicit class ValidatedOps[ErrorType, ValidType](validated: Validated[ErrorType, ValidType]) {

      def flatMap[NewValidType](map: ValidType => Validated[ErrorType, NewValidType]): Validated[ErrorType, NewValidType] =
        validated match {
          case Validated.Valid(validValue) => map(validValue)
          case invalid@Validated.Invalid(_) => invalid
        }
    }
  }
}
