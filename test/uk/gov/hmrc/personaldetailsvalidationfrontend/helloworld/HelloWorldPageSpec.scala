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

package uk.gov.hmrc.personaldetailsvalidationfrontend.helloworld

import org.jsoup.nodes.Document
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.personaldetailsvalidationfrontend.views.ViewSetup
import uk.gov.hmrc.play.test.UnitSpec

class HelloWorldPageSpec extends UnitSpec with OneAppPerSuite {

  "render" should {

    "return a H1 with a 'hello world' message" in new ViewSetup {
      val page: Document = html.hello_world()

      page.select("h1").text() shouldBe "Hello from personal-details-validation-frontend!"
    }
  }
}
