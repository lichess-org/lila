package lila.ui

import play.api.libs.json.{ JsString, JsValue, Json, Writes }

enum HttpMethod:
  case GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD

object HttpMethod:
  given Writes[HttpMethod] = Writes(hm => JsString(hm.toString))

case class Menu(val items: List[MenuItem], val moreLabel: String):
  def serialize: String = Json.stringify(Json.toJson(this))

case class MenuItem(
    label: String,
    icon: Icon,
    href: String,
    category: Option[String] = None,
    cssClass: Option[String] = None,
    httpMethod: Option[HttpMethod] = None
)

object MenuItem:
  given Writes[MenuItem] = Json.writes

object Menu:
  given Writes[Menu] = Json.writes
