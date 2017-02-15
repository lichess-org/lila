package lila.common

import scalaz.Functor

package object paginator {

  implicit val LilaPaginatorFunctor = new Functor[Paginator] {
    def map[A, B](p: Paginator[A])(f: A => B) = new Paginator(
      currentPage = p.currentPage,
      maxPerPage = p.maxPerPage,
      currentPageResults = p.currentPageResults map f,
      nbResults = p.nbResults
    )
  }
}
