package lila.forumSearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.QueryDefinition
import org.elasticsearch.search.sort.SortOrder

import lila.search.ElasticSearch

private[forumSearch] final class Query private (
    indexType: String,
    terms: List[String],
    staff: Boolean,
    troll: Boolean) extends lila.search.Query {

  def searchDef(from: Int = 0, size: Int = 10) =
    search in indexType query makeQuery sort (
      by field Fields.date order SortOrder.DESC
    ) start from size size

  def countDef = count from indexType query makeQuery

  private def queryTerms = terms filterNot (_ startsWith "user:")
  private def userSearch = terms find (_ startsWith "user:") map { _ drop 5 }

  private lazy val makeQuery = filteredQuery query {
    queryTerms match {
      case Nil => all
      case terms => must {
        terms.map { term =>
          multiMatchQuery(term) fields (Query.searchableFields: _*)
        }: _*
      }
    }
  } filter {
    List(
      userSearch map { termFilter(Fields.author, _) },
      !staff option termFilter(Fields.staff, false),
      !troll option termFilter(Fields.troll, false)
    ).flatten match {
        case Nil => matchAllFilter
        case filters => must {
          filters: _*
        }
      }
  }
}

object Query {

  private val searchableFields = List(Fields.body, Fields.topic, Fields.author)

  def apply(indexType: String, text: String, staff: Boolean, troll: Boolean): Query = new Query(
    indexType, ElasticSearch decomposeTextQuery text, staff, troll
  )
}
