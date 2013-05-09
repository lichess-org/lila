package lila.gameSearch

import lila.search.TypeIndexer
import lila.game.{ GameRepo, PgnRepo, Game ⇒ GameModel, Query ⇒ DbQuery }
import lila.db.api.$find
import lila.game.tube.gameTube

import com.typesafe.config.Config
import akka.actor._
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import org.elasticsearch.action.search.SearchResponse

final class Env(
    config: Config,
    system: ActorSystem,
    esIndexer: EsIndexer) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer
  )), name = IndexerName)

  lazy val paginator = new lila.search.PaginatorBuilder(
    indexer = lowLevelIndexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = responseToGames _)

  lazy val forms = new DataForm

  def cli = new lila.common.Cli {
    import akka.pattern.ask
    import lila.search.actorApi.RebuildAll
    private implicit def timeout = makeTimeout minutes 60
    def process = {
      case "game" :: "search" :: "reset" :: Nil ⇒
        (lowLevelIndexer ? RebuildAll) inject "Game search index rebuilt"
    }
  }

  private val lowLevelIndexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    es = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Game.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private def responseToGames(response: SearchResponse): Fu[List[GameModel]] =
    $find.byOrderedIds[GameModel](response.getHits.hits.toList map (_.id))

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.iteratee._
    import lila.db.api._
    import lila.db.Implicits.LilaPimpedQueryBuilder
    val selector = DbQuery.frozen ++ sel
    val query = $query(selector) sort DbQuery.sortCreated 
    val size = $count(selector).await
    val batchSize = 1000
    var nb = 0
    var nbSkipped = 0
    var started = nowMillis
    $enumerate.bulk[Option[GameModel]](query, batchSize) { gameOptions ⇒
      val games = gameOptions.flatten
      val nbGames = games.size
      nb = nb + nbGames
      PgnRepo.associate(games.map(_.id).toSeq) map { pgns ⇒
        val pairs = (pgns map {
          case (id, pgn) ⇒ games.find(_.id == id) map (_ -> pgn)
        }).flatten
        esIndexer bulk_send {
          (pairs map {
            case (game, pgn) ⇒ esIndexer.index_prepare(
              IndexName,
              TypeName,
              game.id,
              Json stringify Game.from(game, pgn)
            ).request
          })
        }
        nbSkipped = nbSkipped + nbGames - pairs.size
        val perMs = batchSize / (nowMillis - started)
        started = nowMillis
        loginfo("[game search] Indexed %d of %d, skipped %d, at %d/s".format(
          nb, size, nbSkipped, math.round(perMs * 1000)))
      }
    }
  }
}

object Env {

  lazy val current = "[boot] gameSearch" describes new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = lila.common.PlayApp.system,
    esIndexer = lila.search.Env.current.esIndexer)
}
