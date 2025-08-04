package lila.common

import play.api.libs.json.{ Json, JsValue, Writes }
import lila.ui.Icon

case class Menu(val items: List[MenuItem], val moreLabel: String):
  def serialize(): String = Json.stringify(Json.toJson(this))

case class MenuItem(
    label: String,
    icon: Icon,
    href: String,
    category: Option[String] = None,
    cssClass: Option[String] = None
)

object MenuItem:
  implicit val writes: Writes[MenuItem] = Json.writes[MenuItem]

  def serialize(items: List[MenuItem]): JsValue = Json.toJson(items)

object Menu:
  implicit val writes: Writes[Menu] = Json.writes[Menu]
