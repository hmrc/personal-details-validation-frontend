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

package uk.gov.hmrc.config.implicits

import cats.data.ValidatedNel
import play.api.Configuration
import support.UnitSpec
import uk.gov.hmrc.config.implicits.*
import uk.gov.hmrc.http.Host

import java.time.Duration

class ConfigImplicitsSpec extends UnitSpec {

  private def config(pairs: (String, Any)*): Configuration =
    Configuration(pairs.map { case (k, v) => k -> v }*)

  "stringValueFinder" should {

    "return Valid when the key is present" in {
      val result: ValidatedNel[String, String] = stringValueFinder("my.key")(config("my.key" -> "hello"))
      result.isValid shouldBe true
      result.getOrElse("") shouldBe "hello"
    }
  }

  "intValueFinder" should {

    "return Valid when the key is present" in {
      val result: ValidatedNel[String, Int] = intValueFinder("my.int")(config("my.int" -> 42))
      result.isValid shouldBe true
      result.getOrElse(0) shouldBe 42
    }
  }

  "stringValuesFinder" should {

    "return Valid(Seq) when the key is present" in {
      val result: ValidatedNel[String, Seq[String]] =
        stringValuesFinder("my.list")(config("my.list" -> List("a", "b")))
      result.isValid shouldBe true
      result.getOrElse(Seq.empty) shouldBe List("a", "b")
    }

    "return Invalid when the key is missing" in {
      val result: ValidatedNel[String, Seq[String]] =
        stringValuesFinder("missing.list")(config())
      result.isValid shouldBe false
      // the error should mention the missing key
      result.toEither.left.get.head should include ("missing.list not found")
    }
  }

  "hostFinder" should {

    "return Valid(Host) with default http protocol" in {
      val cfg = config(
        "microservice.services.protocol" -> "http",
        "microservice.services.my-service.protocol" -> "http",
        "microservice.services.my-service.host" -> "localhost",
        "microservice.services.my-service.port" -> 8080
      )
      val result: ValidatedNel[String, Host] = hostFinder("my-service")(cfg)
      result.isValid shouldBe true
      result.getOrElse(Host("")).value shouldBe "http://localhost:8080"
    }

    "return Valid(Host) with https protocol override" in {
      val cfg = config(
        "microservice.services.protocol" -> "http",
        "microservice.services.my-service.protocol" -> "https",
        "microservice.services.my-service.host" -> "example.com",
        "microservice.services.my-service.port" -> 443
      )
      val result: ValidatedNel[String, Host] = hostFinder("my-service")(cfg)
      result.isValid shouldBe true
      result.getOrElse(Host("")).value shouldBe "https://example.com:443"
    }

    "return Invalid when host is missing" in {
      val cfg = config(
        "microservice.services.protocol" -> "http",
        "microservice.services.my-service.protocol" -> "http",
        // host intentionally missing
        "microservice.services.my-service.port" -> 8080
      )
      val result: ValidatedNel[String, Host] = hostFinder("my-service")(cfg)
      result.isValid shouldBe false
      val errors = result.toEither.left.get
      errors.head should include ("microservice.services.my-service.host not found")
    }

    "return Invalid when port is missing" in {
      val cfg = config(
        "microservice.services.protocol" -> "http",
        "microservice.services.my-service.protocol" -> "http",
        "microservice.services.my-service.host" -> "localhost"
        // port intentionally missing
      )
      val result: ValidatedNel[String, Host] = hostFinder("my-service")(cfg)
      result.isValid shouldBe false
      val errors = result.toEither.left.get
      errors.head should include ("microservice.services.my-service.port not found")
    }

    "return Invalid accumulating both errors when host and port are missing" in {
      val cfg = config(
        "microservice.services.protocol" -> "http",
        "microservice.services.my-service.protocol" -> "http"
        // both host and port intentionally missing
      )
      val result: ValidatedNel[String, Host] = hostFinder("my-service")(cfg)
      result.isValid shouldBe false
      val messages = result.toEither.left.get.toList
      messages.length shouldBe 2
      messages.exists(_.contains("microservice.services.my-service.host not found")) shouldBe true
      messages.exists(_.contains("microservice.services.my-service.port not found")) shouldBe true
    }
  }

  "durationFinder" should {

    "return Valid(Duration) when the key is present with a valid ISO-8601 duration in minutes" in {
      val result: ValidatedNel[String, Duration] =
        durationFinder("my.duration")(config("my.duration" -> "PT5M"))
      result.isValid shouldBe true
      result.getOrElse(Duration.ZERO) shouldBe Duration.ofMinutes(5)
    }

    "return Valid(Duration) when the key is present with a duration in seconds" in {
      val result: ValidatedNel[String, Duration] =
        durationFinder("my.duration")(config("my.duration" -> "PT30S"))
      result.isValid shouldBe true
      result.getOrElse(Duration.ZERO) shouldBe Duration.ofSeconds(30)
    }

    "return Invalid when the key is missing" in {
      val result: ValidatedNel[String, Duration] =
        durationFinder("my.duration")(config())
      result.isValid shouldBe false
      val errors = result.toEither.left.get
      errors.head should include ("my.duration not found")
    }
  }
}
