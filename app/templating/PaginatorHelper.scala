package lila.app
package templating

import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

trait PaginatorHelper {

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
