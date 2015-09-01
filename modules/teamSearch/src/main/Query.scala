package lila.teamSearch

import lila.search.ElasticSearch

import play.api.libs.json._

private[teamSearch] final class Query private (
    terms: List[String]) extends lila.search.Query {

  def toJson = Json.obj()

  //   def searchDef(from: Int = 0, size: Int = 10) =
  //     search in indexType query makeQuery sort (
  //       field sort Fields.nbMembers order SortOrder.DESC
  //     ) start from size size

  //   def countDef = count from indexType query makeQuery

  //   private def makeQuery = terms match {
  //     case Nil => all
  //     case terms => must {
  //       terms.map { term =>
  //         multiMatchQuery(term) fields (Query.searchableFields: _*)
  //       }: _*
  //     }
  //   }
}

object Query {

  private val searchableFields = List(Fields.name, Fields.description, Fields.location)

  def apply(text: String): Query =
    new Query(ElasticSearch decomposeTextQuery text)
}
