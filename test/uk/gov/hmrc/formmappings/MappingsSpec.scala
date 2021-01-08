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

package uk.gov.hmrc.formmappings

import java.time.LocalDate

import generators.Generators.Implicits._
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.data.FormError
import play.api.data.Forms.mapping
import support.UnitSpec
import uk.gov.hmrc.personaldetailsvalidation.model.NonEmptyString

class MappingsSpec
  extends UnitSpec
    with GeneratorDrivenPropertyChecks {

  import Mappings._

  "mandatoryText" should {

    "bind successfully for a given String value" in {
      val bindResult = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .bind(Map("field" -> "some text"))

      bindResult shouldBe Right(NonEmptyString("some text"))
    }

    "trim and bind successfully for a given String value" in {
      val bindResult = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .bind(Map("field" -> " some text  "))

      bindResult shouldBe Right(NonEmptyString("some text"))
    }

    "return the given error if no entry for the field is given" in {
      val bindResult = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .bind(Map.empty)

      bindResult shouldBe Left(Seq(FormError("field", "error")))
    }

    "return the given error if value for the given key is empty" in {
      val bindResult = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .bind(Map("field" -> " "))

      bindResult shouldBe Left(Seq(FormError("field", "error")))
    }

    "unbind the given value" in {
      val unboundValue = mapping("field" -> mandatoryText("error"))(identity)(Some.apply)
        .unbind(NonEmptyString("some text"))

      unboundValue shouldBe Map("field" -> "some text")
    }
  }

  "mandatoryLocalDate.bind" should {

    "bind successfully when given year, month and day are valid and the user is 15 years and 9 months old or above." in new DateMappingSetup with DateMapping {

      legalLocalDates.map { date: LocalDate =>

        val bindResult = dateMapping.bind(Map(
          s"$dateFieldName.year" -> date.getYear.toString,
          s"$dateFieldName.month" -> date.getMonthValue.toString,
          s"$dateFieldName.day" -> date.getDayOfMonth.toString
        ))

        bindResult shouldBe Right(date)
      }
    }

    "return the 'required' errors if some date parts are missing" in new DateMappingSetup with DateMapping {

      val removeAPart: (Seq[(String, String)], Int) => Seq[(String, String)] = {
        case (partsLeft, _) =>
          val partToRemove = Gen.oneOf(partsLeft).generateOne
          partsLeft filterNot (_ == partToRemove)
      }

      forAll(Gen.oneOf(1, 2), validDateParts) { (numberOfPartsToRemove, allParts) =>

        val partsLeft = (1 to numberOfPartsToRemove).foldLeft(allParts)(removeAPart)

        val bindResult = dateMapping.bind(partsLeft.toMap)

        bindResult shouldBe Left(
          (allParts diff partsLeft)
            .map(toPartName)
            .map(toErrorKeySuffixed("required"))
            .map(toFormError)
        )
      }
    }

    "return the 'required' error if all date parts are missing" in new DateMappingSetup with DateMapping {

      val bindResult = dateMapping.bind(Map.empty)

      bindResult shouldBe Left(Seq(FormError(dateFieldName, s"$errorKeyPrefix.$dateFieldName.required")))
    }

    "return the 'required' error if there are blank values for parts" in new DateMappingSetup with DateMapping {

      forAll(generatedPartNames, validDateParts) { (partToBeInvalid, allParts) =>

        val partsWithBlanks = allParts map {
          case (partName, _) if partName == partToBeInvalid => partName -> " "
          case partNameAndValue => partNameAndValue
        }

        val bindResult = dateMapping.bind(partsWithBlanks.toMap)

        bindResult shouldBe Left(
          Seq(partToBeInvalid)
            .map(toErrorKeySuffixed("required"))
            .map(toFormError)
        )
      }
    }

    "return the 'invalid' error if date parts are not parseable to Int" in new DateMappingSetup with DateMapping {

      forAll(generatedPartNames, validDateParts) { (partToBeInvalid, allParts) =>

        val partsWithInvalids = allParts map {
          case (partName, _) if partName == partToBeInvalid => partName -> "abc"
          case partNameAndValue => partNameAndValue
        }

        val bindResult = dateMapping.bind(partsWithInvalids.toMap)

        bindResult shouldBe Left(
          Seq(partToBeInvalid)
            .map(toErrorKeySuffixed("invalid"))
            .map(toFormError)
        )
      }
    }

    "return the 'invalid' error if date parts have wrong values" in new DateMappingSetup with DateMapping {

      forAll(generatedPartNames, validDateParts) { (partToBeInvalid, allParts) =>

        val partWithIllegalValue = partToBeInvalid match {
          case `yearPartName` =>
            yearPartName -> Gen.oneOf(1000 - 1, 9999 + 1).generateOne.toString
          case `monthPartName` =>
            monthPartName -> Gen.oneOf(-1, 0, 13).generateOne.toString
          case `dayPartName` =>
            dayPartName -> Gen.oneOf(-1, 0, 32).generateOne.toString
        }

        val partsWithInvalids = allParts.toMap + partWithIllegalValue

        val bindResult = dateMapping.bind(partsWithInvalids)

        bindResult shouldBe Left(
          Seq(partToBeInvalid)
            .map(toErrorKeySuffixed("invalid"))
            .map(toFormError)
        )
      }
    }

    "return the 'invalid' error if date parts forms invalid date" in new DateMappingSetup with DateMapping {

      val partsWithInvalids = Map(
        s"$dateFieldName.year" -> "2017",
        s"$dateFieldName.month" -> "2",
        s"$dateFieldName.day" -> "29"
      )

      val bindResult = dateMapping.bind(partsWithInvalids)

      bindResult shouldBe Left(Seq(FormError(dateFieldName, s"$errorKeyPrefix.$dateFieldName.invalid")))
    }

    "return the 'invalid' error if date parts are invalid and there are additional constraints added" in new DateMappingSetup {

      val dateMapping = mapping(
        dateFieldName -> mandatoryLocalDate(errorKeyPrefix).verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      val bindResult = dateMapping.bind(Map(
        s"$dateFieldName.year" -> "a",
        s"$dateFieldName.month" -> "b",
        s"$dateFieldName.day" -> "c"
      ))

      bindResult shouldBe Left(
        partNames
          .map(toErrorKeySuffixed("invalid"))
          .map(toFormError)
      )
    }

    "return the additional verification error if date parts are valid but does not meet the additional constraints" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.key").verifying("special.error", _.isAfter(LocalDate.of(2000, 11, 24)))
      )(identity)(Some.apply)

      val bindResult = dateMapping.bind(Map(
        "date.year" -> "2000",
        "date.month" -> "11",
        "date.day" -> "24"
      ))

      bindResult shouldBe Left(Seq(FormError("date", "special.error")))
    }

    "return the 'tooYoung' error if date shows that the user is less than 15 years and 9 months old" in new DateMappingSetup with DateMapping {

      val partsWithInvalids = Map(
        s"$dateFieldName.year" -> "2017",
        s"$dateFieldName.month" -> "2",
        s"$dateFieldName.day" -> "2"
      )

      val bindResult = dateMapping.bind(partsWithInvalids)

      bindResult shouldBe Left(Seq(FormError(dateFieldName, s"$errorKeyPrefix.$dateFieldName.tooYoung")))
    }

  }

  "mandatoryLocalDate.unbind" should {

    "unbind the given LocalDate" in new DateMappingSetup with DateMapping {

      forAll { localDate: LocalDate =>

        dateMapping.unbind(localDate) shouldBe Map(
          s"$dateFieldName.year" -> localDate.getYear.toString,
          s"$dateFieldName.month" -> localDate.getMonthValue.toString,
          s"$dateFieldName.day" -> localDate.getDayOfMonth.toString
        )
      }
    }
  }

  "mandatoryLocalDate.unbindAndValidate" should {

    "return unbound date with errors if the unbound value does not meet defined constraints" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.key").verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      dateMapping.unbindAndValidate(LocalDate.of(2017, 11, 23)) shouldBe Map(
        "date.year" -> "2017",
        "date.month" -> "11",
        "date.day" -> "23"
      ) -> Seq(FormError("date", "special.error"))
    }

    "return unbound date with no errors if the unbound value meets defined constraints" in {

      val dateMapping = mapping(
        "date" -> mandatoryLocalDate("error.key").verifying("special.error", _.isAfter(LocalDate.of(2017, 11, 24)))
      )(identity)(Some.apply)

      dateMapping.unbindAndValidate(LocalDate.of(2017, 11, 25)) shouldBe Map(
        "date.year" -> "2017",
        "date.month" -> "11",
        "date.day" -> "25"
      ) -> Nil
    }
  }

  private trait DateMapping {

    self: DateMappingSetup =>

    val dateMapping = mapping(
      dateFieldName -> mandatoryLocalDate(errorKeyPrefix)
    )(identity)(Some.apply)
  }

  private trait DateMappingSetup extends generators.Generators {

    val dateFieldName = "date"
    val errorKeyPrefix = "error.key"

    val yearPartName = s"$dateFieldName.year"
    val monthPartName = s"$dateFieldName.month"
    val dayPartName = s"$dateFieldName.day"
    val partNames = Seq(yearPartName, monthPartName, dayPartName)
    val generatedPartNames: Gen[String] = Gen.oneOf(partNames).suchThat(partNames.contains)

    val validDateParts: Gen[Seq[(String, String)]] = for {
      date <- legalLocalDates
    } yield Seq(
      s"$dateFieldName.year" -> date.getYear.toString,
      s"$dateFieldName.month" -> date.getMonthValue.toString,
      s"$dateFieldName.day" -> date.getDayOfMonth.toString
    )

    val toPartName: ((String, String)) => String = {
      case (partName, _) => partName
    }

    def toErrorKeySuffixed(suffix: String): String => String =
      name => s"$errorKeyPrefix.$name.$suffix"

    def toFormError(errorKey: String): FormError =
      FormError(dateFieldName, errorKey)
  }
}
