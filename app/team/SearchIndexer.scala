package lila
package team

import search.{ ElasticSearch, TypeIndexer }

import scalaz.effects._
import play.api.libs.json._

import org.elasticsearch.action.search.SearchResponse

import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
import com.mongodb.casbah.query.Imports._

private[team] final class SearchIndexer(es: EsIndexer, teamRepo: TeamRepo) {

  val indexName = "lila"
  val typeName = "team"

  private val indexer = new TypeIndexer(es, typeName, SearchMapping.mapping, indexQuery)

  val rebuildAll = indexer.rebuildAll

  val optimize = indexer.optimize

  val search = indexer.search _

  val count = indexer.count _

  def insertOne(team: Team) = SearchMapping(team) match {
    case (id, doc) ⇒ indexer.insertOne(id, doc)
  }

  def removeOne(team: Team) = indexer removeOne team.id

  private def indexQuery(query: DBObject) {
    val cursor = teamRepo find query
    for (teams ← cursor grouped 5000) {
      indexer.insertMany(teams.map(SearchMapping.apply).toMap).unsafePerformIO
    }
    cursor.count
  }

  def toTeams(response: SearchResponse): IO[List[Team]] = teamRepo byOrderedIds {
    response.hits.hits.toList map (_.id)
  }
}
