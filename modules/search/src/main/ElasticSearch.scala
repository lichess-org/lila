package lila.search

import play.api.libs.json._

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._

object ElasticSearch {

  // synchronously create index and type, ignoring errors (already existing)
  def createType(client: ElasticClient, indexName: String, typeName: String) {
    try {
      client.sync execute {
        create index indexName
      }
    }
    catch {
      case e: Exception => // println("create type: " + e)
    }
    // client.sync execute {
    //   delete from indexName -> typeName where matchall
    // }
    import org.elasticsearch.index.query.QueryBuilders._
    client.java.prepareDeleteByQuery(indexName)
      .setTypes(typeName)
      .setQuery(matchAllQuery)
      .execute()
      .actionGet()
  }

  def decomposeTextQuery(text: String): List[String] =
    text.trim.toLowerCase.replace("+", " ").split(" ").toList

  object Date {

    import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

    val format = "YYYY-MM-dd HH:mm:ss"

    val formatter: DateTimeFormatter = DateTimeFormat forPattern format
  }
}
