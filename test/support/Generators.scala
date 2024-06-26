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

package support

import org.scalacheck.{Arbitrary, Gen}

import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}

trait Generators {

  implicit val booleans: Gen[Boolean] = Gen.oneOf(true, false)

  def probableBoolean(percentTrue: Int): Gen[Boolean] = Gen.choose(1, 100).map(_ <= percentTrue)

  def optional[T](generator: Gen[T], percentSome: Int = 50): Gen[Option[T]] = for {
    isSome <- probableBoolean(percentSome)
    value <- if (isSome) generator.map(Some.apply) else Gen.const(None)
  } yield value

  def positiveInts(max: Int): Gen[Int] = Gen.chooseNum(1, max)

  def longs(max: Long): Gen[Long] = Gen.chooseNum(1L, max)

  def strings(maxLength: Int): Gen[String] = strings(1, maxLength)

  def strings(minLength: Int, maxLength: Int): Gen[String] = {
    for {
      length <- Gen.chooseNum(minLength, maxLength)
      chars <- Gen.listOfN(length, Gen.alphaNumChar)
    } yield chars.mkString
  } suchThat (value => value.length >= minLength && value.length <= maxLength)

  val nonEmptyStrings: Gen[String] = strings(1, 1000)

  implicit val instants: Gen[Instant] = Gen.choose(minTimestamp, maxTimestamp).map(Instant.ofEpochMilli)

  implicit val legalLocalDates: Gen[LocalDate] = instants.map(toDate)

  object Implicits {

    implicit def asArbitrary[T](implicit generator: Gen[T]): Arbitrary[T] = Arbitrary(generator)

    implicit def asGen[T](value: T): Gen[T] = Gen.const(value)

    implicit def generatorOfOps[T](generator: Gen[T]): GenOps[T] = GenOps(generator)

    case class GenOps[T](generator: Gen[T]) {

      lazy val toGenOfOption: Gen[Option[T]] = optional(generator)

      lazy val generateOne: T = generator.sample.get
    }

    case class ArbitraryOps[T](arbitrary: Arbitrary[T]) {

      lazy val toGenOfOption: Arbitrary[Option[T]] = Arbitrary(optional[T](arbitrary.arbitrary))

      lazy val generateOne: T = arbitrary.arbitrary.sample.get
      lazy val generateOptional: Option[Arbitrary[T]] = optional(arbitrary).generateOne
      lazy val generateOneWrappedAsOption: Gen[Option[T]] = arbitrary.arbitrary.map(Option.apply)

      def exclude(values: T*): Gen[T] =
        arbitrary.arbitrary.retryUntil(generated => !values.contains(generated))

      def generateValueDifferentThan(values: T*): T =
        exclude(values: _*)
          .sample.getOrElse {
          throw new IllegalArgumentException(s"Cannot generate value different than: ${values.mkString(", ")}")
        }

      def generateValueDifferentThan(maybeValue: Option[T]): Option[T] =
        Generators.probableBoolean(percentTrue = 33).generateOne match {
          case true => None
          case false => maybeValue match {
            case Some(value) => exclude(value).sample
            case _ => arbitrary.arbitrary.sample
          }
        }
    }

  }

  private val EuropeLondon = ZoneId.of("Europe/London")

  val minTimestamp: Long = Instant.parse("1900-01-01T00:00:00.000Z").toEpochMilli
  val maxTimestamp: Long = Instant.parse("2003-01-01T00:00:00.000Z").toEpochMilli

  def toDate(i: Instant): LocalDate = ZonedDateTime.ofInstant(i, EuropeLondon).toLocalDate

}

object Generators extends Generators
