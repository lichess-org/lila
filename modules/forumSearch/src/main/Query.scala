package lila.forumSearch

import org.elasticsearch.index.query._, QueryBuilders._, FilterBuilders._
import Post.fields

import lila.search.ElasticSearch

private[forumSearch] final class Query private (
    terms: List[String],
    staff: Boolean,
    troll: Boolean) extends lila.search.Query {

  def searchRequest(from: Int = 0, size: Int = 10) = ElasticSearch.Request.Search(
    query = makeQuery,
    filter = makeFilters,
    from = from,
    size = size)

  def countRequest = ElasticSearch.Request.Count(makeQuery, makeFilters)

  private def queryTerms = terms filterNot (_ startsWith "user:")
  private def userSearch = terms find (_ startsWith "user:") flatMap {
    _.drop(5).some.filter(_.size >= 2)
  }

  private lazy val makeQuery =
    if (queryTerms.isEmpty) matchAllQuery
    else queryTerms.foldLeft(boolQuery) {
      case (query, term) => query must {
        multiMatchQuery(term, fields.body, fields.topic, fields.author)
      }
    }

  private lazy val makeFilters = List(
    userSearch map { termFilter(fields.author, _) },
    !staff option termFilter(fields.staff, false),
    !troll option termFilter(fields.troll, false)
  ).flatten.toNel map { fs => andFilter(fs.list: _*) }
}

object Query {

  def apply(text: String, staff: Boolean, troll: Boolean): Query = new Query(
    ElasticSearch.Request decomposeTextQuery text, staff, troll
  )
}
