package lila
package search

import game.{ GameRepo, DbGame, Query ⇒ GameQuery }

import scalaz.effects._
import com.traackr.scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.codahale.jerkson.Json
import com.mongodb.casbah.query.Imports._

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.ActionRequest
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
import org.elasticsearch.index.query._

final class Indexer(es: EsIndexer, gameRepo: GameRepo) {

  val indexName = "lila"
  val typeName = "game"

  def rebuildAll: IO[Unit] = for {
    _ ← clear
    nb ← indexAll
    _ ← io {
      es.waitTillCountAtLeast(Seq(indexName), typeName, nb)
    }
    _ ← optimize
    games ← search(matchAllQuery, 0, 5)
    _ ← putStrLn(games mkString " ")
  } yield ()

  def search(
    query: QueryBuilder,
    from: Int,
    size: Int): IO[List[DbGame]] = toGames {
    es.search(Seq(indexName), Seq(typeName), query,
      from = from.some,
      size = size.some)
  }

  def searchTest = search(matchAllQuery, 0, 5)

  private def searchTestQuery = 

  private def clear: IO[Unit] = io {
    def prop(name: String, typ: String) = name -> Map("type" -> typ)
    val mapping = Json generate Map(
      typeName -> Map(
        "properties" -> List(
          prop("status", "integer"),
          prop("turns", "integer"),
          prop("rated", "boolean"),
          prop("variant", "integer")
        ).toMap
      )
    )
    println("will delete index")
    es.deleteIndex(Seq(indexName))
    println("will create index")
    es.createIndex(indexName, settings = Map("number_of_shards" -> "1"))
    println("will wait till active")
    es.waitTillActive()
    println("will put mapping")
    es.putMapping(indexName, typeName, mapping)
    println("will refresh")
    es.refresh()
    println("done")
  }

  private def indexAll: IO[Int] = io {
    val cursor = gameRepo.collection.find(
      GameQuery.finished,
      DBObject(
        "status" -> true,
        "turns" -> true,
        "rated" -> true,
        "v" -> true
      ))
    var nb = 0
    for (games ← cursor grouped 5000) {
      println("Indexing " + nb)
        val actions = (games map { game ⇒
          for {
            id ← game.getAs[String]("_id")
            status ← game.getAs[Int]("status")
            turns ← game.getAs[Int]("turns")
            rated = game.getAs[Boolean]("isRated") | false
            variant = game.getAs[Int]("v") | 1
          } yield es.index_prepare(indexName, typeName, id, Json generate Map(
            "status" -> status,
            "turns" -> turns,
            "rated" -> rated,
            "variant" -> variant
          )).request
        }).flatten
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

  private def toGames(response: SearchResponse): IO[List[DbGame]] =
    gameRepo games {
      response.hits.hits.toList map (_.id)
    }
}
