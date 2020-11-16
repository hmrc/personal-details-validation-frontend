package uk.gov.hmrc.personaldetailsvalidation.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CREATED, FAILED_DEPENDENCY}
import play.api.libs.json.{JsString, JsValue, Json}
import uk.gov.hmrc.domain.Nino

object PersonalDetailsService {

  case class PersonalDetailsData(firstName: String, lastName: String, nino: Option[Nino] = None, postcode: Option[String] = None, dateOfBirth: LocalDate)

  object PersonalDetails {
    implicit val writes = Json.writes[PersonalDetailsData]
  }

  def validatesSuccessfully(personalDetails: PersonalDetailsData): Unit = validate(personalDetails, validationSuccess = true)
  def validatesUnsuccessfully(personalDetails: PersonalDetailsData): Unit = validate(personalDetails, validationSuccess = false)
  def validatesDeceased(personalDetails: PersonalDetailsData): Unit = validateDeceased(personalDetails)

  private def validate(personalDetails: PersonalDetailsData, validationSuccess: Boolean): Unit = {
    val validationId = UUID.randomUUID().toString

    `POST /personal-details-validation`(personalDetails)
      .toReturn(
        status = CREATED,
        body = Json.obj(
          "validationStatus" -> (if(validationSuccess) "success" else "failure") ,
          "id" -> validationId
        )
      )
  }

  private def validateDeceased(personalDetails: PersonalDetailsData): Unit = {

    `POST /personal-details-validation`(personalDetails)
      .toReturn(
        status = FAILED_DEPENDENCY,
        body = JsString("Request to create account for a deceased user")
      )
  }

  private def `POST /personal-details-validation`(personalDetails: PersonalDetailsData) = new {

    def expectedJson(personalDetailsData: PersonalDetailsService.PersonalDetailsData): String = {
      personalDetailsData match {
        case dataWithNino if dataWithNino.nino.isDefined =>
          s"""{
             | "firstName":"${dataWithNino.firstName}",
             | "lastName":"${dataWithNino.lastName}",
             | "dateOfBirth":"${dataWithNino.dateOfBirth.format(ISO_LOCAL_DATE)}",
             | "nino":"${dataWithNino.nino.get}"
             |}
             | """.stripMargin
        case dataWithPostcode if dataWithPostcode.postcode.isDefined =>
          s"""{
             | "firstName":"${dataWithPostcode.firstName}",
             | "lastName":"${dataWithPostcode.lastName}",
             | "dateOfBirth":"${dataWithPostcode.dateOfBirth.format(ISO_LOCAL_DATE)}",
             | "postCode":"${dataWithPostcode.postcode.get}"
             |}
             | """.stripMargin
      }
    }

    def toReturn(status: Int, body: JsValue): StubMapping =
      stubFor(
        post(urlEqualTo("/personal-details-validation"))
          .withRequestBody(equalToJson(expectedJson(personalDetails), true, false))
          .willReturn(aResponse()
            .withStatus(status)
              .withBody(body.toString())
          ))
  }
}
