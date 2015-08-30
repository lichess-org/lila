package lila.search

import akka.actor.ActorRef
import akka.pattern.ask
import com.sksamuel.elastic4s.ElasticDsl._

import lila.common.paginator._
import makeTimeout.large

final class PaginatorBuilder[A](
    indexer: ActorRef,
    maxPerPage: Int,
    converter: SearchResponse => Fu[List[A]]) {

  def apply(query: Query, page: Int): Fu[Paginator[A]] = Paginator(
    adapter = new ESAdapter(query),
    currentPage = page,
    maxPerPage = maxPerPage)

  def ids(query: Query, max: Int): Fu[List[String]] =
    indexer ? actorApi.Search(query.searchDef(0, max)) map {
      case res: SearchResponse => res.hitIds
    }

  private final class ESAdapter(query: Query) extends AdapterLike[A] {

    def nbResults = indexer ? actorApi.Count(query.countDef) map {
      case res: CountResponse => res.count
    }

    def slice(offset: Int, length: Int) =
      indexer ? actorApi.Search(query.searchDef(offset, length)) flatMap {
        case res: SearchResponse => converter(res)
      }
  }
}
