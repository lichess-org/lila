package lila.teamSearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.QueryDefinition
import org.elasticsearch.search.sort.SortOrder

import lila.search.ElasticSearch

private[teamSearch] final class Query private (
    indexType: String,
    terms: List[String]) extends lila.search.Query {

  def searchDef(from: Int = 0, size: Int = 10) =
    search in indexType query makeQuery sort (
      by field Fields.nbMembers order SortOrder.DESC
    ) start from size size

  def countDef = count from indexType query makeQuery

  private def makeQuery = terms match {
    case Nil => all
    case terms => must {
      terms.map { term =>
        multiMatchQuery(term) fields (Query.searchableFields: _*)
      }: _*
    }
  }
}

object Query {

  private val searchableFields = List(Fields.name, Fields.description, Fields.location)

  def apply(indexType: String, text: String): Query = new Query(
    indexType, ElasticSearch decomposeTextQuery text
  )
}
