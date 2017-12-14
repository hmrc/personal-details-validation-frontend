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

import java.time.LocalDate

import cats.implicits._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{FormError, Mapping, ObjectMapping}

import scala.util.{Failure, Success, Try}

case class LocalDateMapping private[formmappings](key: String = "",
                                                  constraints: Seq[Constraint[LocalDate]] = Nil)
                                                 (formatError: => String)
  extends Mapping[LocalDate] with ObjectMapping {

  val mappings: Seq[Mapping[_]] = Seq(this)

  def bind(data: Map[String, String]): Seq[FormError] Either LocalDate =
    Seq(data.find("year"), data.find("month"), data.find("day"))
      .map(partsToInt)
      .toXorOfSequence
      .flatMap(toLocalDate)
      .leftMap(toFormErrors)
      .flatMap(checkConstraints)

  private implicit class DataOps(data: Map[String, String]) {
    def find(partName: String): String Either String =
      Either.fromOption(data.get(formKeyFor(partName)), s"Missing $partName")
  }

  private def formKeyFor(partName: String) =
    (if (key.isEmpty) None else Some(key))
      .map(k => s"$k.$partName").getOrElse(partName)

  private val partsToInt: (String Either String) => String Either Int =
    _.flatMap { stringDataPart =>
      Try(stringDataPart.toInt) match {
        case Success(intDataPart) => Right(intDataPart)
        case Failure(parsingError) => Left(parsingError.getMessage)
      }
    }

  private implicit class IntsOps(partsAsInts: Seq[String Either Int]) {

    val toXorOfSequence: String Either Seq[Int] =
      partsAsInts
        .foldLeft(Either.right[String, Seq[Int]](Nil)) {
          (partsSeq, maybeDataPart) =>
            partsSeq.flatMap(parts => maybeDataPart.map(part => parts :+ part))
        }
  }

  private val toLocalDate: (Seq[Int]) => String Either LocalDate =
    allParts =>
      if (allParts.head < 1000)
        Left(s"Year ${allParts.head} is invalid")
      else Try {
        LocalDate.of(allParts.head, allParts(1), allParts(2))
      } match {
        case Success(localDate) => Right(localDate)
        case Failure(exception) => Left(exception.getMessage)
      }

  private val checkConstraints: LocalDate => Seq[FormError] Either LocalDate =
    localDate =>
      constraints.map(_.apply(localDate)).foldLeft(Seq.empty[FormError]) {
        case (allErrors, Valid) => allErrors
        case (allErrors, Invalid(errors)) => allErrors ++: errors.map(error => FormError(key, error.message, error.args))
      } match {
        case Nil => Right(localDate)
        case validationErrors => Left(validationErrors)
      }

  private val toFormErrors: String => Seq[FormError] =
    _ => Seq(FormError(key, formatError))

  def unbind(date: LocalDate): Map[String, String] = Map(
    formKeyFor("year") -> date.getYear.toString,
    formKeyFor("month") -> date.getMonthValue.toString,
    formKeyFor("day") -> date.getDayOfMonth.toString
  )

  def unbindAndValidate(date: LocalDate): (Map[String, String], Seq[FormError]) =
    unbind(date) -> collectErrors(date)

  def withPrefix(prefix: String): Mapping[LocalDate] =
    addPrefix(prefix)
      .map(newKey => LocalDateMapping(newKey, constraints)(formatError))
      .getOrElse(this)

  def verifying(addConstraints: Constraint[LocalDate]*): Mapping[LocalDate] =
    LocalDateMapping(key, constraints ++ addConstraints.toSeq)(formatError)
}