package lila
package search

import game.{ GameRepo, DbGame, Query ⇒ GameQuery }

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse

import com.traackr.scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

final class Indexer(
    es: EsIndexer,
    gameRepo: GameRepo,
    queue: Queue) {

  val indexName = "lila"
  val typeName = "game"

  def rebuildAll: IO[Unit] = for {
    _ ← clear
    nb ← indexQuery(DBObject())
    _ ← io { es.waitTillCountAtLeast(Seq(indexName), typeName, nb) }
    _ ← optimize
  } yield ()

  def search(request: SearchRequest): SearchResponse = request.in(indexName, typeName)(es)

  def count(request: CountRequest): Int = request.in(indexName, typeName)(es)

  def index(game: DbGame): IO[Unit] = (Game from game).fold(
    doc ⇒ (for {
      _ ← putStrLn("Search indexing game " + game.id + " as " + doc)
      _ ← io(es.index(indexName, typeName, game.id, Json generate doc))
      _ ← optimize
    } yield ()) except { e ⇒
      putStrLn("Search index: fail to index game " + game.id + " - " + e.getMessage)
    },
    putStrLn("Search index: fail to produce a document from game " + game.id)
  )

  def indexQueue: IO[Unit] = for {
    ids ← queue next 1000
    _ ← ids.toNel.fold(
      neIds ⇒ for {
        _ ← putStrLn("Search indexing %d games" format neIds.list.size)
        _ ← indexQuery("_id" $in neIds.list)
        _ ← queue remove neIds.list
      } yield (),
      io()
    )
  } yield ()

  private def clear: IO[Unit] = io {
    es.deleteIndex(Seq(indexName))
    es.createIndex(indexName, settings = Map())
    es.waitTillActive()
    es.putMapping(indexName, typeName, Json generate Map(typeName -> Game.mapping))
    es.refresh()
  }

  private def indexQuery(query: DBObject): IO[Int] = io {
    val cursor = gameRepo find (GameQuery.finished ++ query) sort GameQuery.sortCreated //limit 3000
    var nb = 0
    for (games ← cursor grouped 5000) {
      println("Indexing from %d to %d".format(nb, nb + games.size))
      val actions = games map (_.decode flatMap Game.from) collect {
        case Some((id, doc)) ⇒
          es.index_prepare(indexName, typeName, id, Json generate doc).request
      }
      if (actions.nonEmpty) {
        es bulk actions
        nb = nb + actions.size
      }
    }
    nb
  }

  private def optimize: IO[Unit] = io {
    es.optimize(Seq(indexName))
  }

  def toGames(response: SearchResponse): IO[List[DbGame]] =
    gameRepo games {
      response.hits.hits.toList map (_.id)
    }
}
