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

package setups.connectors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue, Writes}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.integration.servicemanager.AhcWsClientFactory
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

trait HttpClientStubSetup extends MockFactory {

  private val configuration = mock[Configuration]
  (configuration.getString(_: String, _: Option[Set[String]]))
    .expects("appName", None)
    .returning(Some("personal-details-validation"))

  protected def expectPost(toUrl: String) = new {
    def withPayload(payload: JsObject) = new {

      def returning(status: Int): Unit =
        returning(HttpResponse(status, ""))

      def returning(status: Int, body: JsValue): Unit =
        returning(HttpResponse(status, json = body, Map.empty))

      def returning(status: Int, body: String): Unit =
        returning(HttpResponse(status, body))

      def returning(status: Int, headers: (String, String)*): Unit =
        returning(HttpResponse(
          status,
          "",
          headers = headers.toMap.mapValues(List.apply(_))
        ))

      def returning(response: HttpResponse): Unit =
        httpClient.postStubbing = (actualUrl: String, actualPayload: JsObject) => Future.successful {
          actualUrl shouldBe toUrl
          actualPayload shouldBe payload
          response
        }

      def throwing(exception: RuntimeException): Unit =
        httpClient.postStubbing = (actualUrl: String, actualPayload: JsObject) => Future.failed {
          actualUrl shouldBe toUrl
          actualPayload shouldBe payload
          exception
        }
    }
  }

  protected def expectGet(toUrl: String) = new {

    def returning(status: Int, body: JsValue): Unit =
      returning(HttpResponse(status, json = body, Map.empty))

    def returning(status: Int): Unit =
      returning(HttpResponse(status, ""))

    def returning(status: Int, body: String): Unit =
      returning(HttpResponse(status, body))

    def returning(response: HttpResponse): Unit =
      httpClient.getStubbing = (actualUrl: String) => Future.successful {
        actualUrl shouldBe toUrl
        response
      }

    def throwing(exception: RuntimeException): Unit =
      httpClient.getStubbing = (actualUrl: String) => Future.failed {
        actualUrl shouldBe toUrl
        exception
      }
  }

  class HttpClientStub
    extends HttpClient
      with WSHttp {

    implicit val mat: ActorMaterializer = ActorMaterializer()(actorSystem)

    override val wsClient: WSClient = AhcWsClientFactory.createClient()

    override val hooks: Seq[HttpHook] = Nil

    private[HttpClientStubSetup] var postStubbing: (String, JsObject) => Future[HttpResponse] =
      (_, _) => throw new IllegalStateException("HttpClientStub not configured")

    private[HttpClientStubSetup] var getStubbing: (String) => Future[HttpResponse] =
      (_) => throw new IllegalStateException("HttpClientStub not configured")

    override def doPost[A](url: String, body: A, headers: Seq[(String, String)])
                          (implicit wts: Writes[A], hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
      postStubbing(url, body.asInstanceOf[JsObject])

    override def doGet(url: String, headers: Seq[(String, String)])
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
      getStubbing(url)

    override protected def actorSystem: ActorSystem = ActorSystem()

//    override protected def configuration: Config = None
    override protected def configuration: Config = ConfigFactory.load()
  }

  val httpClient: HttpClientStub = new HttpClientStub()
}
