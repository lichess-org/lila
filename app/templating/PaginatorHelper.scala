package lila.app
package templating

import lila.app.ui.ScalatagsTemplate.*

trait PaginatorHelper:

  def pagerNext(pager: scalalib.paginator.Paginator[?], url: Int => String): Option[Tag] =
    pager.nextPage.map: np =>
      div(cls := "pager")(pagerA(url(np)))

  def pagerNextTable(pager: scalalib.paginator.Paginator[?], url: Int => String): Option[Tag] =
    pager.nextPage.map: np =>
      tr(cls := "pager")(th(pagerA(url(np))))

  private def pagerA(url: String) = a(rel := "next", href := url)("Next")
