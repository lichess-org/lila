package lila.i18n

import org.joda.time.DateTime

private[i18n] case class Translation(
    _id: Int,
    code: String, // 2-chars code
    text: String,
    author: Option[String] = None,
    comment: Option[String] = None,
    createdAt: DateTime) {

  def id = _id

  def lines = text.split("\n").toList

  override def toString = "#%d %s".format(id, code)
}

private[i18n] object Translation {

  import play.api.libs.json._

  private[i18n] implicit val translationI18nFormat = Json.format[Translation]
}
