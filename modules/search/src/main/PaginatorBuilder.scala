package lila.search

import akka.actor.ActorRef

import lila.common.paginator._
import makeTimeout.large

final class PaginatorBuilder[A](
    indexer: ActorRef,
    maxPerPage: Int,
    converter: Seq[String] => Fu[List[A]]) {

  def apply(query: Query, page: Int): Fu[Paginator[A]] = Paginator(
    adapter = new ESAdapter(query),
    currentPage = page,
    maxPerPage = maxPerPage)

  def ids(query: Query, max: Int): Fu[List[String]] = fuccess(Nil)

  private final class ESAdapter(query: Query) extends AdapterLike[A] {

    def nbResults = fuccess(0)

    def slice(offset: Int, length: Int) = fuccess(Nil)
  }
}
