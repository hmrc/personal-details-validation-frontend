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

package uk.gov.hmrc.personaldetailsvalidation.repository

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.Done
import reactivemongo.play.json.ImplicitBSONHandlers._
import com.google.inject.ImplementedBy
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Descending
import reactivemongo.bson.BSONDocument
import uk.gov.hmrc.datetime.CurrentTimeProvider
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.mongoEntity
import uk.gov.hmrc.personaldetailsvalidation.model.RelativeUrl.relativeUrl
import uk.gov.hmrc.personaldetailsvalidation.model.{JourneyId, RelativeUrl}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.json.ops._

@ImplementedBy(classOf[JourneyMongoRepository])
private[personaldetailsvalidation] trait JourneyRepository {

  def persist(journeyIdAndRelativeUrl: (JourneyId, RelativeUrl))
             (implicit executionContext: ExecutionContext): Future[Done]

  def journeyExists(journeyId: JourneyId)
                   (implicit executionContext: ExecutionContext): Future[Boolean]
}

@Singleton
private[personaldetailsvalidation] class JourneyMongoRepository @Inject()(config: JourneyMongoRepositoryConfig, mongoComponent: ReactiveMongoComponent)(implicit currentTimeProvider: CurrentTimeProvider)
  extends ReactiveRepository[(JourneyId, RelativeUrl), JourneyId](
    collectionName = "journey",
    mongo = mongoComponent.mongoConnector.db,
    domainFormat = mongoEntity(JourneyMongoRepository.journeyFormat),
    idFormat = JourneyMongoRepository.journeyIdFormat
  ) with JourneyRepository {

  override def indexes: Seq[Index] = Seq(
    Index(Seq("createdAt" -> Descending), name = Some("journey-ttl-index"), options = BSONDocument("expireAfterSeconds" -> config.collectionTtl.getSeconds))
  )


  override def persist(journeyIdAndRelativeUrl: (JourneyId, RelativeUrl))
                      (implicit executionContext: ExecutionContext): Future[Done] = {
    val document = domainFormatImplicit.writes(journeyIdAndRelativeUrl).as[JsObject].withCreatedTimeStamp()

    collection.insert(document).map(_ => Done)
  }

  override def journeyExists(journeyId: JourneyId)
                            (implicit executionContext: ExecutionContext): Future[Boolean] =
    findById(journeyId) map {
      case Some(_) => true
      case None => false
    }
}

private object JourneyMongoRepository {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.Reads._
  import play.api.libs.json._
  import uk.gov.voa.valuetype.play.formats.ValueTypeFormat._

  private implicit val relativeUrlFormat: Format[RelativeUrl] = {
    format[String, RelativeUrl] {
      value => relativeUrl(value).fold(throw _, identity)
    }
  }

  private[JourneyMongoRepository] implicit val journeyIdFormat: Format[JourneyId] = format[UUID, JourneyId](
    uuid => JourneyId(uuid))(
    parse = {
      case JsString(value) => UUID.fromString(value)
      case x => throw new IllegalArgumentException(s"Expected a JsString, received $x")
    },
    toJson = uuid => JsString(uuid.toString))

  private[JourneyMongoRepository] val journeyFormat: Format[(JourneyId, RelativeUrl)] = {

    val reads: Reads[(JourneyId, RelativeUrl)] = (
      (__ \ "id").read[JourneyId] and
        (__ \ "relativeUrl").read[RelativeUrl]
      ).tupled

    val writes: Writes[(JourneyId, RelativeUrl)] = (
      (__ \ "id").write[JourneyId] and
        (__ \ "relativeUrl").write[RelativeUrl]
      ).tupled

    Format(reads, writes)
  }
}
