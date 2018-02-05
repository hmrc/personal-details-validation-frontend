package uk.gov.hmrc.http

import javax.inject.{Inject, Singleton}

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

@Singleton
class PersonalDetailsValidationFilters @Inject()
(
  frontendFilters: FrontendFilters,
  addGaUserIdInHeaderFilter: AddGaUserIdInHeaderFilter)
  extends HttpFilters {

  override val filters: Seq[EssentialFilter] = frontendFilters.filters :+ addGaUserIdInHeaderFilter
}
