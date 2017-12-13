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

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.data.FormError
import play.api.data.Forms.mapping
import uk.gov.hmrc.personaldetailsvalidationfrontend.generators.Generators.Implicits._
import uk.gov.hmrc.personaldetailsvalidationfrontend.generators.Generators._
import uk.gov.hmrc.play.test.UnitSpec

class MappingsSpec
  extends UnitSpec
    with GeneratorDrivenPropertyChecks {

  import Mappings._

  "mandatoryText" should {

    "bind successfully for a given String value" in {
      val bindResult = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .bind(Map("field" -> "some text"))

      bindResult shouldBe Right("some text")
    }

    "return the given error if no entry for the field is given" in {
      val bindResult = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .bind(Map.empty)

      bindResult shouldBe Left(Seq(FormError("field", "error")))
    }

    "unbind the given value" in {
      val unboundValue = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .unbind("some text")

      unboundValue shouldBe Map("field" -> "some text")
    }
  }

  "mandatoryLocalDate" should {

    "bind successfully when given year, month and day are valid" in new MandatoryLocalDateTestCase {

      forAll { localDate: LocalDate =>

        val bindResult = dateMapping.bind(Map(
          "date.year" -> localDate.getYear.toString,
          "date.month" -> localDate.getMonthValue.toString,
          "date.day" -> localDate.getDayOfMonth.toString
        ))

        bindResult shouldBe Right(localDate)
      }
    }

    "return the given error if there are missing date parts" in new MandatoryLocalDateTestCase {

      forAll(Gen.choose(1, 3), localDates) { (partsNumber, localDate) =>

        val allParts = Seq(
          "date.year" -> localDate.getYear.toString,
          "date.month" -> localDate.getMonthValue.toString,
          "date.day" -> localDate.getDayOfMonth.toString
        )
        val selectedParts = (1 to partsNumber).foldLeft(allParts) { (partsLeft, _) =>
          val partToRemove = Gen.oneOf(partsLeft).generateOne
          partsLeft filterNot (_ == partToRemove)
        }

        val bindResult = dateMapping.bind(selectedParts.toMap)

        bindResult shouldBe Left(Seq(FormError("date", "error.missing")))
      }
    }

    "return the given error if date parts are not parseable to Int" in new MandatoryLocalDateTestCase {

      forAll(Gen.choose(1, 3), localDates) { case (nonIntPart, localDate) =>

        val dateParts = Seq(
          "date.year" -> localDate.getYear.toString,
          "date.month" -> localDate.getMonthValue.toString,
          "date.day" -> localDate.getDayOfMonth.toString
        ).zipWithIndex.map { case (part@(partName, _), idx) =>
          if (idx + 1 == nonIntPart) partName -> "abc"
          else part
        }

        val bindResult = dateMapping.bind(dateParts.toMap)

        bindResult shouldBe Left(Seq(FormError("date", "error.missing")))
      }
    }

    "return the given error if date parts have wrong values" in new MandatoryLocalDateTestCase {

      forAll(Gen.choose(1, 3), localDates) { (invalidIntPart, localDate) =>

        val dateParts = Seq(
          "date.year" -> localDate.getYear.toString,
          "date.month" -> localDate.getMonthValue.toString,
          "date.day" -> localDate.getDayOfMonth.toString
        ).map {
          case ("date.year", _) if invalidIntPart == 1 => "date.year" -> Gen.oneOf(-10000, 999).generateOne.toString
          case ("date.month", _) if invalidIntPart == 2 => "date.month" -> Gen.oneOf(-1, 0, 13).generateOne.toString
          case ("date.day", _) if invalidIntPart == 3 => "date.day" -> Gen.oneOf(-1, 0, 32).generateOne.toString
          case part => part
        }

        val bindResult = dateMapping.bind(dateParts.toMap)

        bindResult shouldBe Left(Seq(FormError("date", "error.missing")))
      }
    }

    "return the given error if date parts are invalid and there are additional constraints added" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.missing").verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      val bindResult = dateMapping.bind(Map(
        "date.year" -> "a",
        "date.month" -> "b",
        "date.day" -> "c"
      ))

      bindResult shouldBe Left(Seq(FormError("date", "error.missing")))
    }

    "return the additional verification error if date parts are valid but does not meet the additional constraints" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.missing").verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      val bindResult = dateMapping.bind(Map(
        "date.year" -> "2017",
        "date.month" -> "11",
        "date.day" -> "24"
      ))

      bindResult shouldBe Left(Seq(FormError("date", "special.error")))
    }

    "unbind the given LocalDate" in new MandatoryLocalDateTestCase {

      forAll { localDate: LocalDate =>

        dateMapping.unbind(localDate) shouldBe Map(
          "date.year" -> localDate.getYear.toString,
          "date.month" -> localDate.getMonthValue.toString,
          "date.day" -> localDate.getDayOfMonth.toString
        )
      }
    }

    "unbindAndValidate the given value and return errors if the unbound value does not meet defined constraints" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.missing").verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      dateMapping.unbindAndValidate(LocalDate.of(2017, 11, 23)) shouldBe Map(
        "date.year" -> "2017",
        "date.month" -> "11",
        "date.day" -> "23"
      ) -> Seq(FormError("date", "special.error"))
    }

    "unbindAndValidate the given value and return no errors if the unbound value meets defined constraints" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.missing").verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      dateMapping.unbindAndValidate(LocalDate.of(2017, 11, 25)) shouldBe Map(
        "date.year" -> "2017",
        "date.month" -> "11",
        "date.day" -> "25"
      ) -> Nil
    }
  }

  private trait MandatoryLocalDateTestCase {

    val dateMapping = mapping(
      "date" -> mandatoryLocalDate("error.missing")
    )(identity)(Some.apply)
  }
}
