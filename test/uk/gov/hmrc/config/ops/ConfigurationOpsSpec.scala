/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.config.ops

import cats.data.ValidatedNel
import play.api.Configuration
import support.UnitSpec
import uk.gov.hmrc.config.ops.*

class ConfigurationOpsSpec extends UnitSpec {

  private def config(pairs: (String, Any)*): Configuration =
    Configuration(pairs.map { case (k, v) => k -> v }*)

  "loadMandatory" should {

    "return the value when the key is present" in {
      implicit val finder: String => Configuration => ValidatedNel[String, String] =
        key => cfg => cats.data.Validated.validNel(cfg.get[String](key))

      val result = config("my.key" -> "hello").loadMandatory[String]("my.key")
      result shouldBe "hello"
    }

    "throw a RuntimeException when the key is missing" in {
      implicit val finder: String => Configuration => ValidatedNel[String, String] =
        key => _ => cats.data.Validated.invalidNel(s"$key not found")

      a[RuntimeException] should be thrownBy config().loadMandatory[String]("missing.key")
    }
  }

  "load" should {

    "return the value when the key is present" in {
      implicit val finder: String => Configuration => ValidatedNel[String, String] =
        key => cfg => cats.data.Validated.validNel(cfg.get[String](key))

      val result = config("my.key" -> "found").load[String]("my.key", "default")
      result shouldBe "found"
    }

    "return the default when the key is missing" in {
      implicit val finder: String => Configuration => ValidatedNel[String, String] =
        key => _ => cats.data.Validated.invalidNel(s"$key not found")

      val result = config().load[String]("missing.key", "default-value")
      result shouldBe "default-value"
    }
  }

  "loadOptional" should {

    "return Some(value) when the key is present" in {
      implicit val finder: String => Configuration => ValidatedNel[String, String] =
        key => cfg => cats.data.Validated.validNel(cfg.get[String](key))

      val result = config("my.key" -> "optional").loadOptional[String]("my.key")
      result shouldBe Some("optional")
    }

    "return None when the key is missing" in {
      implicit val finder: String => Configuration => ValidatedNel[String, String] =
        key => _ => cats.data.Validated.invalidNel(s"$key not found")

      val result = config().loadOptional[String]("missing.key")
      result shouldBe None
    }
  }
}
