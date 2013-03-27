package lila.common

import scalaz.Functor

package object paginator {

  implicit def LilaPaginatorFunctor = new Functor[Paginator] {
    def fmap[A, B](p: Paginator[A], f: A ⇒ B) = new Paginator(
      currentPage = p.currentPage,
      maxPerPage = p.maxPerPage,
      currentPageResults = p.currentPageResults map f,
      nbResults = p.nbResults)
  }
}
