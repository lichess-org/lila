package lila.search

import lila.common.paginator._

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.libs.concurrent.Execution.Implicits._
import org.elasticsearch.action.search.SearchResponse

final class PaginatorBuilder[A](
    indexer: ActorRef,
    maxPerPage: Int,
    converter: SearchResponse ⇒ Fu[List[A]]) {

  def apply(query: Query, page: Int): Fu[Paginator[A]] = Paginator(
    adapter = new ESAdapter(query),
    currentPage = page,
    maxPerPage = maxPerPage)

  private final class ESAdapter(query: Query) extends AdapterLike[A] {

    private implicit val timeout = makeTimeout.large

    def nbResults = indexer ? actorApi.Count(query.countRequest) map {
      case actorApi.CountResponse(res) ⇒ res
    }

    def slice(offset: Int, length: Int) =
      indexer ? actorApi.Search(query.searchRequest(offset, length)) flatMap {
        case actorApi.SearchResponse(res) ⇒ converter(res)
      }
  }
}
