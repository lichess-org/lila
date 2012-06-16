package lila
package i18n

import com.novus.salat.annotations.Key
import org.joda.time.DateTime

case class Translation(
  @Key("_id") id: Int,
  code: String, // 2-chars code
  text: String,
  author: Option[String],
  comment: Option[String],
  createdAt: DateTime = DateTime.now) {

    def toMap = Map(
      "id" -> id,
      "code" -> code,
      "text" -> text,
      "author" -> author,
      "comment" -> comment,
      "createdAt" -> createdAt.getMillis)

    def lines = text.split("\n").toList
}
