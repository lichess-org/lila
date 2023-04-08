package lila.common
package paginator

import play.api.libs.json.*

object PaginatorJson:

  given Writes[config.MaxPerPage] with
    def writes(m: config.MaxPerPage) = JsNumber(m.value)

  given [A: Writes]: Writes[Paginator[A]] = Writes[Paginator[A]](apply)

  def apply[A: Writes](p: Paginator[A]): JsObject =
    Json.obj(
      "currentPage"        -> p.currentPage,
      "maxPerPage"         -> p.maxPerPage,
      "currentPageResults" -> p.currentPageResults,
      "nbResults"          -> p.nbResults,
      "previousPage"       -> p.previousPage,
      "nextPage"           -> p.nextPage,
      "nbPages"            -> p.nbPages
    )
