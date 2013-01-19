package lila
package search

object ElasticSearch {

  object Date {

    import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

    val format = "YYYY-MM-dd HH:mm:ss"

    val formatter: DateTimeFormatter = DateTimeFormat forPattern format
  }

  object Mapping {

    def field(name: String, typ: String, analyzed: Boolean = false, attrs: Map[String, Any] = Map.empty) =
      name -> (Map(
        "type" -> typ,
        "index" -> analyzed.fold("analyzed", "not_analyzed")
      ) ++ attrs)

    def boost(name: String, typ: String, b: Int = 1, attrs: Map[String, Any] = Map.empty) =
      field(name, typ, true, Map("boost" -> b) ++ attrs)

    def obj(name: String, properties: Map[String, Any]) =
      name -> Map("type" -> "object", "properties" -> properties)
  }

  object Request {

    import org.elasticsearch.action.search.SearchResponse
    import org.elasticsearch.action.ActionRequest
    import org.elasticsearch.action.search.SearchType
    import org.elasticsearch.index.query.QueryBuilder
    import org.elasticsearch.index.query._, FilterBuilders._, QueryBuilders._
    import org.elasticsearch.search._, facet._, terms._, sort._, SortBuilders._, builder._

    import scalastic.elasticsearch.{ Indexer ⇒ EsIndexer }
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
      }.hits.totalHits.toInt
    }

  }
}
