package lila.app
package search

import ElasticSearch.Request
import game.{ GameRepo, PgnRepo, DbGame, Query ⇒ GameQuery }

import scalaz.effects._

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._
import play.api.libs.json.Json

final class GameIndexer(
    es: EsIndexer,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    queue: Queue) {

  val indexName = "lila"
  val typeName = "game"

  private val indexer = new TypeIndexer(es, typeName, Game.jsonMapping, indexQuery)

  val rebuildAll = indexer.rebuildAll

  val optimize = indexer.optimize

  val search = indexer.search _

  val count = indexer.count _

  val indexQueue: IO[Unit] = queue next 2000 flatMap { ids ⇒
    ~ids.toNel.map(neIds ⇒
      putStrLn("[search] indexing %d games" format neIds.list.size) >>
        io(indexQuery("_id" $in neIds.list)) >>
        (queue remove neIds.list)
    )
  }

  private def indexQuery(query: DBObject) {
    val cursor = gameRepo find (GameQuery.frozen ++ query) sort GameQuery.sortCreated //limit 3000
    val size = cursor.count
    var nb = 0
    for (games ← cursor grouped 5000) {
      val pgns = games.map(g ⇒ (pgnRepo get g.id).unsafePerformIO)
      val gamesWithPgn = games zip pgns
      if (size > 1000) println("Indexing %d of %d".format(nb, size))
      val actions = gamesWithPgn map {
        case (game, pgn) ⇒ game.decode map Game.from(pgn)
      } collect {
        case Some((id, doc)) ⇒
          es.index_prepare(indexName, typeName, id, Json stringify doc).request
      }
      if (actions.nonEmpty) {
        es bulk actions
        nb = nb + actions.size
      }
    }
    nb
  }

  def toGames(response: SearchResponse): IO[List[DbGame]] = gameRepo games {
    response.hits.hits.toList map (_.id)
  }
}
