package lila.ui

import play.api.libs.json.{ Json, JsValue, Writes }

case class Menu(val items: List[MenuItem], val moreLabel: String):
  def serialize: String = Json.stringify(Json.toJson(this))

case class MenuItem(
    label: String,
    icon: Icon,
    href: String,
    category: Option[String] = None,
    cssClass: Option[String] = None
)

object MenuItem:
  given Writes[MenuItem] = Json.writes

object Menu:
  given Writes[Menu] = Json.writes
