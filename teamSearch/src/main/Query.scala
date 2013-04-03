package lila.teamSearch

import lila.search.ElasticSearch
import Team.fields

import org.elasticsearch.index.query._, QueryBuilders._

private[teamSearch] final class Query private (terms: List[String]) extends lila.search.Query {

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery)

  private def makeQuery = terms.foldLeft(boolQuery()) {
    case (query, term) ⇒ query must {
      multiMatchQuery(term, fields.name, fields.description, fields.location)
    }
  }
}

object Query {

  def apply(text: String): Query = new Query(
    ElasticSearch.Request decomposeTextQuery text
  )
}
