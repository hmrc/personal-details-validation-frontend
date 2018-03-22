/*
 * Copyright 2018 HM Revenue & Customs
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

import uk.gov.hmrc.errorhandling.ProcessingError

trait QueryParamSerializer[QP] {
  def serialize(queryParam: QP): Map[String, Seq[String]]
}

object QueryParamSerializer {

  implicit class QueryParamSerializerOps[QP](target: QP) {
    def serialize(implicit serializer: QueryParamSerializer[QP]): Map[String, Seq[String]] = serializer.serialize(target)
  }

  implicit val validationId: QueryParamSerializer[ValidationId] = new QueryParamSerializer[ValidationId] {
    override def serialize(queryParam: ValidationId) = Map("validationId" -> Seq(queryParam.value))
  }

  implicit val technicalError: QueryParamSerializer[ProcessingError] = new QueryParamSerializer[ProcessingError] {
    override def serialize(queryParam: ProcessingError) = {
      Map("technicalError" -> Seq(""))
    }
  }

}

