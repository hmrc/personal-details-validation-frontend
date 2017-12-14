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

package uk.gov.hmrc.personaldetailsvalidation.endpoints.verifiers

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import uk.gov.hmrc.errorhandling.ErrorHandler
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyId
import uk.gov.hmrc.personaldetailsvalidation.repository.JourneyRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
private[endpoints] class JourneyIdVerifier @Inject()(private val errorHandler: ErrorHandler,
                                                     private val journeyRepository: JourneyRepository) {

  def forExisting(journeyId: JourneyId): ActionFilter[Request] with ActionBuilder[Request] = new ActionFilter[Request] with ActionBuilder[Request] {

    import Results._

    override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
      implicit val executionContext: ExecutionContext = request.toExecutionContext

      journeyRepository.journeyExists(journeyId) map {
        case true => None
        case false => Some(NotFound(errorHandler.internalServerErrorTemplate(request)))
      }
    }
  }

  private implicit class RequestOps(request: Request[_]) {

    import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
    import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

    lazy val toExecutionContext: ExecutionContext =
      fromLoggingDetails(headerCarrier(request))

    private def headerCarrier(rh: RequestHeader): HeaderCarrier =
      fromHeadersAndSession(rh.headers, Some(rh.session))
  }
}
