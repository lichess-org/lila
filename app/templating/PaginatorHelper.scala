package lila.app
package templating

import lila.common.paginator.Paginator

trait PaginatorHelper {

  implicit def toRichPager[A](pager: Paginator[A]) = new {

    def sliding(length: Int, showPost: Boolean = true): List[Option[Int]] = {
      val fromPage = 1 max (pager.currentPage - length)
      val toPage = pager.nbPages min (pager.currentPage + length)
      val pre = fromPage match {
        case 1 => Nil
        case 2 => List(1.some)
        case x => List(1.some, none)
      }
      val post = toPage match {
        case x if x == pager.nbPages     => Nil
        case x if x == pager.nbPages - 1 => List(pager.nbPages.some)
        case x if showPost               => List(none, pager.nbPages.some)
        case _                           => List(none)
      }
      pre ::: (fromPage to toPage).toList.map(some) ::: post
    }

    def firstIndex: Int =
      (pager.maxPerPage * (pager.currentPage - 1) + 1) min pager.nbResults

    def lastIndex: Int =
      (firstIndex + pageNbResults - 1) max 0

    def pageNbResults =
      pager.currentPageResults.size
  }
}
