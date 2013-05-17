package lila.forumSearch

import lila.search.ElasticSearch
import Post.fields

import org.elasticsearch.index.query._, QueryBuilders._, FilterBuilders._

private[forumSearch] final class Query private (
    terms: List[String],
    staff: Boolean,
    troll: Boolean) extends lila.search.Query {

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    filter = makeFilters,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery)

  private def makeQuery = terms.foldLeft(boolQuery()) {
    case (query, term) ⇒ query must {
      multiMatchQuery(term, fields.body, fields.topic, fields.author)
    }
  }

  private def makeFilters = List(
    !staff option termFilter(fields.staff, false),
    !troll option termFilter(fields.troll, false)
  ).flatten.toNel map { fs ⇒ andFilter(fs.list: _*) }
}

object Query {

  def apply(text: String, staff: Boolean, troll: Boolean): Query = new Query(
    ElasticSearch.Request decomposeTextQuery text, staff, troll
  )
}
