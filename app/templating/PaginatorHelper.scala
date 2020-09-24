package lila.app
package templating

import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

trait PaginatorHelper {

  implicit def toRichPager[A](pager: Paginator[A]): RichPager = new RichPager(pager)

  def pagerNext(pager: lila.common.paginator.Paginator[_], url: Int => String): Option[Tag] =
    pager.nextPage.map { np =>
      div(cls := "pager")(pagerA(url(np)))
    }

  def pagerNextTable(pager: lila.common.paginator.Paginator[_], url: Int => String): Option[Tag] =
    pager.nextPage.map { np =>
      tr(cls := "pager")(th(pagerA(url(np))))
    }

  private def pagerA(url: String) = a(rel := "next", href := url)("Next")
}

final class RichPager(pager: Paginator[_]) {

  def sliding(length: Int, showPost: Boolean = true): List[Option[Int]] = {
    val fromPage = 1 max (pager.currentPage - length)
    val toPage   = pager.nbPages min (pager.currentPage + length)
    val pre = fromPage match {
      case 1 => Nil
      case 2 => List(1.some)
      case _ => List(1.some, none)
    }
    val post = toPage match {
      case x if x == pager.nbPages     => Nil
      case x if x == pager.nbPages - 1 => List(pager.nbPages.some)
      case _ if showPost               => List(none, pager.nbPages.some)
      case _                           => List(none)
    }
    pre ::: (fromPage to toPage).view.map(some).toList ::: post
  }
}
