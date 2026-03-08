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

package uk.gov.hmrc.personaldetailsvalidation.model

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.UnitSpec

class LoginOriginHelperSpec extends UnitSpec {

  "isDwp(loginOrigin: String)" should {

    "return true for a DWP origin" in {
      LoginOriginHelper.isDwp("dwp-iv") shouldBe true
    }

    "return true for a DWP origin regardless of case" in {
      LoginOriginHelper.isDwp("DWP-IV") shouldBe true
      LoginOriginHelper.isDwp("Dwp-Iv") shouldBe true
    }

    "return true for a DWP origin with suffix" in {
      LoginOriginHelper.isDwp("dwp-iv-something") shouldBe true
    }

    "return false for a non-DWP origin" in {
      LoginOriginHelper.isDwp("pta") shouldBe false
      LoginOriginHelper.isDwp("bta") shouldBe false
      LoginOriginHelper.isDwp("ma")  shouldBe false
    }
  }

  "isDwp(implicit request)" should {

    "return true when the session contains a DWP origin" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "dwp-iv"))
      LoginOriginHelper.isDwp shouldBe true
    }

    "return false when the session contains a non-DWP origin" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "pta"))
      LoginOriginHelper.isDwp shouldBe false
    }

    "return false when the session has no origin key" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      LoginOriginHelper.isDwp shouldBe false
    }
  }

  "isNotDwp" should {

    "return false when origin is DWP" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "dwp-iv"))
      LoginOriginHelper.isNotDwp shouldBe false
    }

    "return true when origin is not DWP" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "pta"))
      LoginOriginHelper.isNotDwp shouldBe true
    }
  }

  "isSa(loginOrigin: String)" should {

    "return true for an SA-suffixed origin" in {
      LoginOriginHelper.isSa("bta-sa") shouldBe true
      LoginOriginHelper.isSa("pta-sa") shouldBe true
    }

    "return false for a non-SA origin" in {
      LoginOriginHelper.isSa("dwp-iv") shouldBe false
      LoginOriginHelper.isSa("ma")     shouldBe false
    }
  }

  "isDwpOrSa(loginOrigin: String)" should {

    "return true for a DWP origin" in {
      LoginOriginHelper.isDwpOrSa("dwp-iv") shouldBe true
    }

    "return true for an SA origin" in {
      LoginOriginHelper.isDwpOrSa("bta-sa") shouldBe true
    }

    "return false for neither DWP nor SA" in {
      LoginOriginHelper.isDwpOrSa("ma") shouldBe false
    }
  }

  "isDwpOrSa(implicit request)" should {

    "return true when the session origin is DWP" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "dwp-iv"))
      LoginOriginHelper.isDwpOrSa shouldBe true
    }

    "return true when the session origin is SA" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "pta-sa"))
      LoginOriginHelper.isDwpOrSa shouldBe true
    }

    "return false when the session has no origin key" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      LoginOriginHelper.isDwpOrSa shouldBe false
    }
  }

  "isNotDwpOrSa" should {

    "return false for DWP origin" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "dwp-iv"))
      LoginOriginHelper.isNotDwpOrSa shouldBe false
    }

    "return true for a non-DWP non-SA origin" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(("origin", "ma"))
      LoginOriginHelper.isNotDwpOrSa shouldBe true
    }
  }

  "isDeskPro(loginOrigin: String)" should {

    "return false for a DWP origin" in {
      LoginOriginHelper.isDeskPro("dwp-iv") shouldBe false
    }

    "return false for bta-sa" in {
      LoginOriginHelper.isDeskPro("bta-sa") shouldBe false
    }

    "return false for pta-sa" in {
      LoginOriginHelper.isDeskPro("pta-sa") shouldBe false
    }

    "return false for ssttp-sa" in {
      LoginOriginHelper.isDeskPro("ssttp-sa") shouldBe false
    }

    "return true for a standard non-excluded origin" in {
      LoginOriginHelper.isDeskPro("ma")  shouldBe true
      LoginOriginHelper.isDeskPro("pta") shouldBe true
      LoginOriginHelper.isDeskPro("bta") shouldBe true
    }
  }
}
