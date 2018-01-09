package uk.gov.hmrc.personaldetailsvalidation.services

import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.{CREATED, OK}
import uk.gov.hmrc.personaldetailsvalidation.model.PersonalDetails

object PersonalDetailsService {

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
          .withRequestBody(equalToJson(
            s"""{
               | "firstName":"${personalDetails.firstName}",
               | "lastName":"${personalDetails.lastName}",
               | "dateOfBirth":"${personalDetails.dateOfBirth.format(ISO_LOCAL_DATE)}",
               | "nino":"${personalDetails.nino}"
               |}
               | """.stripMargin, true, false))
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
