package lila.common

import play.api.libs.json.{Json, OWrites}
import lila.common.PimpedJson._

case class LightUser(id: String, name: String, title: Option[String]) {

  def titleName = title.fold(name)(_ + " " + name)
  def titleNameHtml = title.fold(name)(_ + "&nbsp;" + name)
}

object LightUser {

  implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj("id" -> u.id, "name" -> u.name, "title" -> u.title).noNull
  }

  type Getter = String => Option[LightUser]
}
