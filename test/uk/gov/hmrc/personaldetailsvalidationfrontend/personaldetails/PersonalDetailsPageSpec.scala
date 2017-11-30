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

package uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails

import org.jsoup.nodes.Document
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.personaldetailsvalidationfrontend.views.ViewSetup
import uk.gov.hmrc.play.test.UnitSpec

class PersonalDetailsPageSpec extends UnitSpec with OneAppPerSuite {

  "render" should {

    "return a personal details page containing first name, last name, nino and date of birth controls" in new Setup {
      html.title() shouldBe messages("personal-details.title")

      html.select(".faded-text strong").text() shouldBe messages("personal-details.faded-heading")
      html.select(".faded-text ~ header h1").text() shouldBe messages("personal-details.header")
      html.select("header ~ p").text() shouldBe messages("personal-details.paragraph")

      val fieldsets = html.select("form fieldset")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("label[for=firstname] .form-label-bold").text() shouldBe messages("personal-details.firstname")
      firstNameFieldset.select("label[for=firstname] input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("label[for=lastname] .form-label-bold").text() shouldBe messages("personal-details.lastname")
      lastNameFieldset.select("label[for=lastname] input[type=text][name=lastName]").isEmpty shouldBe false

      val ninoFieldset = fieldsets.next()
      ninoFieldset.select("label[for=nino] .form-label-bold").text() shouldBe messages("personal-details.nino")
      val ninoHints = ninoFieldset.select("label[for=nino] .form-hint .form-hint")
      ninoHints.first().text() shouldBe messages("personal-details.nino.hint")
      ninoHints.next().text() shouldBe messages("personal-details.nino.hint.example")
      ninoFieldset.select("label[for=nino] input[type=text][name=nino]").isEmpty shouldBe false

      val dateFieldset = fieldsets.next().select("div fieldset")
      dateFieldset.select(".form-label-bold").text() shouldBe messages("personal-details.dateOfBirth")
      dateFieldset.select(".form-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")
      val dateElementDivs = dateFieldset.select("div")
      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth.day] span").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("label[for=dateOfBirth.day] input[type=number][name=dateOfBirth.day]").isEmpty shouldBe false
      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth.month] span").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("label[for=dateOfBirth.month] input[type=number][name=dateOfBirth.month]").isEmpty shouldBe false
      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth.year] span").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("label[for=dateOfBirth.year] input[type=number][name=dateOfBirth.year]").isEmpty shouldBe false
    }
  }

  private trait Setup extends ViewSetup {
    val html: Document = new PersonalDetailsPage().render
  }
}
