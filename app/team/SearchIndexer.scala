package lila
package team

import search.{ ElasticSearch, TypeIndexer }

import scalaz.effects._
import com.codahale.jerkson.Json

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

final class SearchIndexer(es: EsIndexer, teamRepo: TeamRepo) {

  val indexName = "lila"
  val typeName = "team"

  private val indexer = new TypeIndexer(es, typeName, SearchMapping.mapping)

  val rebuildAll = indexer.rebuildAll(indexQuery)

  val optimize = indexer.optimize

  val search = indexer.search _

  val count = indexer.count _

  // def count(request: CountRequest): Int = request.in(indexName, typeName)(es)

  private def indexQuery(query: DBObject): IO[Int] = io {
    val cursor = teamRepo find query
    for (teams ← cursor grouped 5000) {
      es bulk {
        teams map SearchMapping.apply map {
          case (id, doc) ⇒ es.index_prepare(indexName, typeName, id, Json generate doc).request
        }
      }
    }
    cursor.count
  }

  def toTeams(response: SearchResponse): IO[List[Team]] = teamRepo byOrderedIds {
    response.hits.hits.toList map (_.id)
  }
}
