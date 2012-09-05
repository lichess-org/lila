package lila
package search

import game.{ GameRepo, DbGame, Query ⇒ GameQuery }

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse

import com.traackr.scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }

final class Indexer(es: EsIndexer, gameRepo: GameRepo) {

  val indexName = "lila"
  val typeName = "game"

  def rebuildAll: IO[Unit] = for {
    _ ← clear
    nb ← indexAll
    _ ← io { es.waitTillCountAtLeast(Seq(indexName), typeName, nb) }
    _ ← optimize
  } yield ()

  def search(request: SearchRequest): SearchResponse = request.in(indexName, typeName)(es)

  def count(request: CountRequest): Int = request.in(indexName, typeName)(es)

  private def clear: IO[Unit] = io {
    es.deleteIndex(Seq(indexName))
    es.createIndex(indexName, settings = Map())
    es.waitTillActive()
    es.putMapping(indexName, typeName, Json generate Map(typeName -> Game.mapping))
    es.refresh()
  }

  private def indexAll: IO[Int] = io {
    val cursor = gameRepo find GameQuery.finished sort GameQuery.sortCreated //limit 3000
    var nb = 0
    for (games ← cursor grouped 5000) {
      println("Indexing " + nb)
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
