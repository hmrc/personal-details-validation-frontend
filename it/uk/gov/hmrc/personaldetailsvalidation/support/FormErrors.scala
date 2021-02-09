package uk.gov.hmrc.personaldetailsvalidation.support

import org.openqa.selenium.By

import scala.util.Try

trait FormErrors  {
  self: WebPage =>

  type Field = String
  type Error = String

  def summaryErrors: List[Error] = findAll(cssSelector(".error-summary--show ul li, .govuk-error-summary__list li")).map(_.text).toList
  def summaryErrorsHeading: String = findAll(cssSelector("#error-summary-heading, .govuk-error-summary__title")).map(_.text).toList.mkString

  def fieldErrors: Map[Field, Error] = findAll(cssSelector("div.form-group>label.form-field--error")).foldLeft(Map.empty[String, String]) { (result, label) =>
    val findError = Try(label.underlying.findElement(By.cssSelector("span.error-notification")))
    val fieldError = findError.map(element => Map(label.attribute("for").get -> element.getText)).getOrElse(Map.empty)
    result ++ fieldError
  }
}
