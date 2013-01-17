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

    def obj(name: String, properties: Map[String, Any]) =
      name -> Map("type" -> "object", "properties" -> properties)
  }
}
