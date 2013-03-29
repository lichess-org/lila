package lila.i18n

import org.joda.time.DateTime

private[i18n] case class Translation(
    id: Int,
    code: String, // 2-chars code
    text: String,
    author: Option[String] = None,
    comment: Option[String] = None,
    createdAt: DateTime) {

  def lines = text.split("\n").toList
}
