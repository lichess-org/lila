package lila
package team

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

final class Indexer(es: EsIndexer, teamRepo: TeamRepo) {

  val indexName = "lila"
  val typeName = "team"

  def rebuildAll: IO[Unit] = for {
    _ ← clear
    nb ← indexQuery(DBObject())
    _ ← io { es.waitTillCountAtLeast(Seq(indexName), typeName, nb) }
    _ ← optimize
  } yield ()

  // def search(request: SearchRequest): SearchResponse = request.in(indexName, typeName)(es)

  // def count(request: CountRequest): Int = request.in(indexName, typeName)(es)

  private def clear: IO[Unit] = io {
    es.deleteIndex(Seq(indexName))
  } inject () except { e ⇒ putStrLn("Index does not exist yet") } map { _ ⇒
    es.createIndex(indexName, settings = Map())
    es.waitTillActive()
    es.putMapping(indexName, typeName, Json generate Map(typeName -> SearchMapping.mapping))
    es.refresh()
  }

  private def indexQuery(query: DBObject): IO[Int] = io {
    val cursor = teamRepo find query 
    val size = cursor.count
    var nb = 0
    for (teams ← cursor grouped 5000) {
      val actions = teams map SearchMapping.apply map {
        case (id, doc) ⇒ es.index_prepare(indexName, typeName, id, Json generate doc).request
      }
      if (actions.nonEmpty) {
        es bulk actions
        nb = nb + actions.size
      }
    }
    nb
  }

  val optimize: IO[Unit] = io {
    es.optimize(Seq(indexName))
  }

  // def toTeams(response: SearchResponse): IO[List[DbTeam]] =
  //   teamRepo teams {
  //     response.hits.hits.toList map (_.id)
  //   }
}
