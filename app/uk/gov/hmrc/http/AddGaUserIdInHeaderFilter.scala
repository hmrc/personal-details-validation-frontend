package uk.gov.hmrc.http

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderNames.googleAnalyticUserId

import scala.concurrent.Future

@Singleton
class AddGaUserIdInHeaderFilter @Inject()(implicit val mat: Materializer) extends Filter {

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val gaCookieValue = rh.cookies.get("_ga").map(_.value)
    lazy val gaUserIdInHeader = rh.headers.get(googleAnalyticUserId)

    val newHeaders = gaCookieValue.orElse(gaUserIdInHeader).foldLeft(rh.headers) { case (headers, userId) =>
      headers.replace(googleAnalyticUserId -> userId)
    }
    f(rh.copy(headers = newHeaders))
  }
}
