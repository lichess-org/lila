package lila.teamSearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.QueryDefinition
import org.elasticsearch.search.sort.SortOrder

import lila.search.ElasticSearch

private[teamSearch] final class Query private (
    terms: List[String]) extends lila.search.Query {

  def searchDef(from: Int = 0, size: Int = 10) = indexType =>
    search in indexType query makeQuery sort (
      field sort Fields.nbMembers order SortOrder.DESC
    ) start from size size

  def countDef = indexType => count from indexType query makeQuery

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

  def apply(text: String): Query = new Query(ElasticSearch decomposeTextQuery text)
}
