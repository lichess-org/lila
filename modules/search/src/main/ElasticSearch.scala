package lila.search

import play.api.libs.json._

object ElasticSearch {

  object Date {

    import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

    val format = "YYYY-MM-dd HH:mm:ss"

    val formatter: DateTimeFormatter = DateTimeFormat forPattern format
  }

  object Mapping {

    def field(name: String, typ: String, analyzed: Boolean = false, attrs: JsObject = Json.obj()) =
      name -> (Json.obj(
        "type" -> typ,
        "index" -> analyzed.fold("analyzed", "not_analyzed")
      ) ++ attrs)

    def boost(name: String, typ: String, b: Int = 1, attrs: JsObject = Json.obj()) =
      field(name, typ, true, Json.obj("boost" -> b) ++ attrs)

    def obj(name: String, properties: JsObject) =
      name -> Json.obj("type" -> "object", "properties" -> properties)
  }

  object Request {

    import org.elasticsearch.action.search.SearchResponse
    import org.elasticsearch.action.ActionRequest
    import org.elasticsearch.action.search.SearchType
    import org.elasticsearch.index.query.QueryBuilder
    import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
    import org.elasticsearch.search._, facet._, terms._, sort._, SortBuilders._, builder._

    import scalastic.elasticsearch.{ Indexer => EsIndexer }
    import scalastic.elasticsearch.SearchParameterTypes

    case class Search(
        query: QueryBuilder,
        filter: Option[FilterBuilder] = None,
        size: Int = 10,
        from: Int = 0,
        sortings: Iterable[SearchParameterTypes.Sorting] = Nil) {

      val explain = none[Boolean]

      def in(indexName: String, typeName: String)(es: EsIndexer): SearchResponse =
        es.search(Seq(indexName), Seq(typeName), query,
          filter = filter,
          sortings = sortings,
          from = from.some,
          size = size.some,
          explain = explain
        )
    }

    case class Count(
        query: QueryBuilder,
        filter: Option[FilterBuilder] = None) {

      def in(indexName: String, typeName: String)(es: EsIndexer): Int = {
        es.search(Seq(indexName), Seq(typeName), query,
          filter = filter,
          searchType = SearchType.COUNT.some
        )
      }.getHits.totalHits.toInt
    }

    def decomposeTextQuery(text: String): List[String] = 
      text.trim.toLowerCase.replace("+", " ").split(" ").toList
  }
}
