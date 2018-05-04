package lila.common

import play.api.libs.json.{ Json, OWrites }

case class LightUser(
    id: String,
    name: String,
    title: Option[String],
    isPatron: Boolean
) {

  def titleName = title.fold(name)(_ + " " + name)
  def titleNameHtml = title.fold(name)(_ + "&nbsp;" + name)

  def isBot = title has LightUser.botTitle
}

object LightUser {

  val botTitle = "BOT"

  implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj(
      "id" -> u.id,
      "name" -> u.name
    ).add("title" -> u.title)
      .add("patron" -> u.isPatron)
  }

  type Getter = String => Fu[Option[LightUser]]
  type GetterSync = String => Option[LightUser]
  type IsBotSync = String => Boolean
}
