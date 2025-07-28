package lila.ui

import scalalib.paginator.Paginator

import lila.ui.ScalatagsTemplate.{ *, given }

trait PaginatorHelper:

  def paginationByQuery(route: Call, pager: Paginator[?], showPost: Boolean): Option[Frag] =
    pagination(page => s"$route?page=$page", pager, showPost)

  def pagination(url: Int => String, pager: Paginator[?], showPost: Boolean): Option[Frag] =
    pager.hasToPaginate.option(pagination(url, pager.currentPage, pager.nbPages, showPost))

  def pagination(url: Int => String, page: Int, nbPages: Int, showPost: Boolean): Tag =
    st.nav(cls := "pagination")(
      if page > 1
      then a(href := url(page - 1), dataIcon := Icon.LessThan)
      else span(cls := "disabled", dataIcon := Icon.LessThan),
      sliding(page, nbPages, 3, showPost = showPost).map:
        case None => raw(" &hellip; ")
        case Some(p) if p == page => span(cls := "current")(p)
        case Some(p) => a(href := url(p))(p)
      ,
      if page < nbPages
      then a(rel := "next", dataIcon := Icon.GreaterThan, href := url(page + 1))
      else span(cls := "disabled", dataIcon := Icon.GreaterThan)
    )

  def pagerNext(pager: Paginator[?], url: Int => String): Option[Tag] =
    pager.nextPage.map: np =>
      div(cls := "pager")(pagerA(url(np)))

  def pagerNextTable(pager: Paginator[?], url: Int => String): Option[Tag] =
    pager.nextPage.map: np =>
      tr(cls := "pager")(th(pagerA(url(np))))

  private def pagerA(url: String) = a(rel := "next", href := url)("Next")

  private def sliding(page: Int, nbPages: Int, length: Int, showPost: Boolean): List[Option[Int]] =
    val fromPage = 1.max(page - length)
    val toPage = nbPages.min(page + length)
    val pre = fromPage match
      case 1 => Nil
      case 2 => List(1.some)
      case _ => List(1.some, none)
    val post = toPage match
      case x if x == nbPages => Nil
      case x if x == nbPages - 1 => List(nbPages.some)
      case _ if showPost => List(none, nbPages.some)
      case _ => List(none)
    pre ::: (fromPage to toPage).view.map(some).toList ::: post
