package uk.gov.hmrc.personaldetailsvalidation.services

import java.time.LocalDate
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino

object PersonalDetailsService {

  case class PersonalDetails(firstName: String, lastName:String, nino: Option[Nino] = None, postcode: Option[String] = None, dateOfBirth: LocalDate)

  object PersonalDetails {
    implicit val writes = Json.writes[PersonalDetails]
  }

  def validatesSuccessfully(personalDetails: PersonalDetails): Unit = {
    val validationId = UUID.randomUUID().toString

    `POST /personal-details-validation`(personalDetails)
      .toReturn(
        status = CREATED,
        header = LOCATION -> s"/personal-details-validation?validationId=$validationId"
      )

    `GET /personal-details-validation?validationId=`(validationId)
      .toReturn(OK)
  }

  private def `POST /personal-details-validation`(personalDetails: PersonalDetails) = new {

    def toReturn(status: Int, header: (String, String)) =
      stubFor(
        post(urlEqualTo("/personal-details-validation"))
          .withRequestBody(equalToJson(Json.toJson(personalDetails).toString()))
          .willReturn(aResponse()
            .withStatus(status)
            .withHeader(header._1, header._2)
          ))
  }

  private def `GET /personal-details-validation?validationId=`(validationId: String) = new {
    def toReturn(status: Int) =
      stubFor(
        get(urlEqualTo(s"/personal-details-validation?validationId=$validationId"))
          .willReturn(aResponse()
            .withStatus(OK)
            .withBody(s""" |{"id":"$validationId"}""".stripMargin)
          ))
  }
}
