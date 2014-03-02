package lila.teamSearch

import org.elasticsearch.index.query._, QueryBuilders._
import Team.fields

import lila.search.ElasticSearch

private[teamSearch] final class Query private (terms: List[String]) extends lila.search.Query {

  def searchDef(from: Int = 0, size: Int = 10) = ???
  // ElasticSearch.Request.Search(
  //   query = makeQuery,
  //   from = from,
  //   size = size)

  def countDef = ???
  //ElasticSearch.Request.Count(makeQuery)

  private def makeQuery = terms.foldLeft(boolQuery()) {
    case (query, term) => query must {
      multiMatchQuery(term, fields.name, fields.description, fields.location)
    }
  }
}

object Query {

  def apply(text: String): Query = new Query(
    ElasticSearch.Request decomposeTextQuery text
  )
}
