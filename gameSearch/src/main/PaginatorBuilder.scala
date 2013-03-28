package lila.gameSearch

import lila.game.{ Game ⇒ GameModel }

import lila.common.paginator._
import lila.common.PimpedJson._
import lila.db.paginator._
import lila.db.Implicits._
import lila.search.actorApi

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._
import org.joda.time.DateTime
import org.elasticsearch.action.search.SearchResponse

final class PaginatorBuilder(
    indexer: ActorRef,
    maxPerPage: Int,
    converter: SearchResponse ⇒ Fu[List[GameModel]]) {

  def apply(query: Query, page: Int): Fu[Paginator[GameModel]] = Paginator(
    adapter = new ESAdapter(query),
    currentPage = page,
    maxPerPage = maxPerPage)

  private final class ESAdapter(query: Query) extends AdapterLike[GameModel] {

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
