package lila.ui

import scalalib.paginator.Paginator

import lila.ui.ScalatagsTemplate.*

trait PaginatorHelper:

  def pagerNext(pager: Paginator[?], url: Int => String): Option[Tag] =
    pager.nextPage.map: np =>
      div(cls := "pager")(pagerA(url(np)))

  def pagerNextTable(pager: Paginator[?], url: Int => String): Option[Tag] =
    pager.nextPage.map: np =>
      tr(cls := "pager")(th(pagerA(url(np))))

  private def pagerA(url: String) = a(rel := "next", href := url)("Next")
