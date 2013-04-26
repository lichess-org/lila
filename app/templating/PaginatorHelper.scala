package lila.app
package templating

import lila.common.paginator.Paginator

trait PaginatorHelper {

  implicit def toRichPager[A](pager: Paginator[A]) = new {

    def sliding: List[Int] = {
      val length = 7
      val fromPage = 1 max (pager.currentPage - length)
      val toPage = pager.nbPages min (pager.currentPage + length)
      (fromPage to toPage).toList
    }

    def firstIndex: Int =
      (pager.maxPerPage * (pager.currentPage - 1) + 1) min pager.nbResults

    def lastIndex: Int =
      (firstIndex + pageNbResults - 1) max 0

    def pageNbResults =
      pager.currentPageResults.size
  }
}
