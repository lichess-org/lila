package lila.gameSearch

import akka.actor._
import com.typesafe.config.Config
import org.elasticsearch.action.search.SearchResponse
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }

import lila.db.api.$find
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game ⇒ GameModel, Query ⇒ DbQuery }
import lila.search.TypeIndexer

final class Env(
    config: Config,
    system: ActorSystem,
    esIndexer: Fu[EsIndexer],
    analyser: lila.analyse.Analyser) {

  private val IndexName = config getString "index"
  private val TypeName = config getString "type"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  private val IndexerName = config getString "indexer.name"

  private val lowLevelIndexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    esIndexer = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Game.jsonMapping,
    indexQuery = indexQuery _
  )), name = IndexerName + "-low-level")

  private val indexer: ActorRef = system.actorOf(Props(new Indexer(
    lowLevel = lowLevelIndexer,
    isAnalyzed = analyser.has _
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
    esIndexer map { es ⇒
      $enumerate.bulk[Option[GameModel]](query, batchSize) { gameOptions ⇒
        val games = gameOptions.flatten
        val gameIds = games.map(_.id).toSeq
        val nbGames = games.size
        nb = nb + nbGames
        GameRepo.associatePgn(gameIds) flatMap { pgns ⇒
          analyser hasMany gameIds map { analysedIds ⇒
            val pairs = (pgns map {
              case (id, pgn) ⇒ games.find(_.id == id) map (_ -> pgn)
            }).flatten
            es bulk_send {
              (pairs map {
                case (game, moves) ⇒ es.index_prepare(
                  IndexName,
                  TypeName,
                  game.id,
                  Json stringify Game.from(game, analysedIds contains game.id)
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
  }
}

object Env {

  lazy val current = "[boot] gameSearch" describes new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = lila.common.PlayApp.system,
    esIndexer = lila.search.Env.current.esIndexer,
    analyser = lila.analyse.Env.current.analyser)
}
