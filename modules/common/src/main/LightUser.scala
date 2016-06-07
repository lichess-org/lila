package lila.common

import lila.common.PimpedJson._
import play.api.libs.json.{ Json, OWrites }

case class LightUser(
    id: String,
    name: String,
    title: Option[String],
    patron: Option[Int]) {

  def titleName = title.fold(name)(_ + " " + name)
  def titleNameHtml = title.fold(name)(_ + "&nbsp;" + name)

  def isPatron = patron.isDefined || id.size % 3 == 0
}

object LightUser {

  implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj(
      "id" -> u.id,
      "name" -> u.name,
      "title" -> u.title,
      "patron" -> u.patron).noNull
  }

  type Getter = String => Option[LightUser]
}
