package lila
package i18n

import com.novus.salat.annotations.Key
import org.joda.time.DateTime

case class Translation(
  @Key("_id") id: Int,
  code: String, // 2-chars code
  yaml: String,
  author: Option[String],
  comment: Option[String],
  createdAt: DateTime) {

}
