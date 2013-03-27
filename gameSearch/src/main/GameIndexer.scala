package lila.gameSearch

import lila.search.ElasticSearch.Request
import lila.search.TypeIndexer
import lila.search.actorApi._
import lila.game.{ GameRepo, PgnRepo, Game => GameModel }

import org.elasticsearch.action.search.SearchResponse
import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }

import akka.actor._
import play.api.libs.json._

final class GameIndexer(
    es: EsIndexer,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    indexName: String,
    typeName: String) extends Actor {

  private val indexer = context.actorOf(Props(
    new TypeIndexer(es, indexName, typeName, Game.jsonMapping, indexQuery)
  ), name = "game-type-indexer")

  def receive = {

    case message => indexer forward message
  }

  // val indexQueue: IO[Unit] = queue next 2000 flatMap { ids ⇒
  //   ~ids.toNel.map(neIds ⇒
  //     putStrLn("[search] indexing %d games" format neIds.list.size) >>
  //       io(indexQuery("_id" $in neIds.list)) >>
  //       (queue remove neIds.list)
  //   )
  // }

  private def indexQuery(query: JsObject) {
    val cursor = gameRepo find (GameQuery.frozen ++ query) sort GameQuery.sortCreated //limit 3000
    val size = cursor.count
    var nb = 0
    // for (games ← cursor grouped 5000) {
    //   val pgns = games.map(g ⇒ (pgnRepo get g.id).unsafePerformIO)
    //   val gamesWithPgn = games zip pgns
    //   if (size > 1000) println("Indexing %d of %d".format(nb, size))
    //   val actions = gamesWithPgn map {
    //     case (game, pgn) ⇒ game.decode map Game.from(pgn)
    //   } collect {
    //     case Some((id, doc)) ⇒
    //       es.index_prepare(indexName, typeName, id, Json stringify doc).request
    //   }
    //   if (actions.nonEmpty) {
    //     es bulk actions
    //     nb = nb + actions.size
    //   }
    // }
    // nb
  }

  // def toGames(response: SearchResponse): IO[List[DbGame]] = gameRepo games {
  //   response.hits.hits.toList map (_.id)
  // }
}
