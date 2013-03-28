package lila.gameSearch

import lila.search.TypeIndexer
import lila.game.{ GameRepo, PgnRepo, Game ⇒ GameModel, Query ⇒ DbQuery }

import com.typesafe.config.Config
import akka.actor._
import play.api.libs.json.JsObject
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import org.elasticsearch.action.search.SearchResponse

final class Env(
  config: Config,
  system: ActorSystem,
  esIndexer: EsIndexer,
  gameRepo: GameRepo,
  pgnRepo: PgnRepo) {

  val IndexName = config getString "index"
  val TypeName = config getString "type"
  val PaginatorMaxPerPage = config getInt "paginator.max_per_page"

  val indexer: ActorRef = system.actorOf(Props(new TypeIndexer(
    es = esIndexer,
    indexName = IndexName,
    typeName = TypeName,
    mapping = Game.jsonMapping,
    indexQuery = indexQuery _
  )), name = "game-type-indexer")

  lazy val paginatorBuilder = new PaginatorBuilder(
    indexer = indexer,
    maxPerPage = PaginatorMaxPerPage,
    converter = responseToGames _)

  lazy val forms = new DataForm

  private def responseToGames(response: SearchResponse): Fu[List[GameModel]] = 
    gameRepo.find byIds {
      response.hits.hits.toList map (_.id)
    }

  private def indexQuery(sel: JsObject): Funit = {
    import play.api.libs.json._
    import play.api.libs.concurrent.Execution.Implicits._
    import play.api.libs.iteratee._
    import lila.db.api.Full.{ query, count }
    import lila.db.Implicits.LilaPimpedQueryBuilder
    import gameRepo.{ coll, bsonReader }
    val selector = DbQuery.frozen ++ sel
    val cursor = query(selector).sort(DbQuery.sortCreated).cursor //limit 3000
    val size = count(selector).await
    var nb = 0
    cursor.enumerateBulks(5000) run {
      Iteratee foreach { (gameOptions: Iterator[Option[GameModel]]) ⇒
        val games = gameOptions.flatten
        nb = nb + games.size
        if (size > 1000) println(s"Index $nb of $size games")
        // #TODO fetch all pgns in one request
        val pgns = games.map(g ⇒ (pgnRepo get g.id).await)
        esIndexer bulk {
          games zip pgns map {
            case (game, pgn) ⇒ esIndexer.index_prepare(
              IndexName,
              TypeName,
              game.id,
              Json stringify Game.from(game, pgn)
            ).request
          } toList
        }
      }
    }
  } 
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "gameSearch",
    system = play.api.libs.concurrent.Akka.system(play.api.Play.current),
    esIndexer = lila.search.Env.current.esIndexer,
    gameRepo = lila.game.Env.current.gameRepo,
    pgnRepo = lila.game.Env.current.pgnRepo)
}
