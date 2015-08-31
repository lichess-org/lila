package lila.search

import play.api.libs.json._

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._

object ElasticSearch {


  def decomposeTextQuery(text: String): List[String] =
    text.trim.toLowerCase.replace("+", " ").split(" ").toList

  object Date {

    import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }

    val format = "YYYY-MM-dd HH:mm:ss"

    val formatter: DateTimeFormatter = DateTimeFormat forPattern format
  }
}
