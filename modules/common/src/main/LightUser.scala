package lila.common

import play.api.libs.json.{ Json, OWrites }

case class LightUser(
    id: String,
    name: String,
    title: Option[String],
    isPatron: Boolean
) {

  def titleName = title.fold(name)(_ + " " + name)

  def isBot = title has "BOT"
}

object LightUser {

  implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json.obj(
      "id" -> u.id,
      "name" -> u.name
    ).add("title" -> u.title)
      .add("patron" -> u.isPatron)
  }

  def fallback(userId: String) = LightUser(
    id = userId,
    name = userId,
    title = None,
    isPatron = false
  )

  final class Getter(f: String => Fu[Option[LightUser]]) extends (String => Fu[Option[LightUser]]) {
    def apply(u: String) = f(u)
  }

  final class GetterSync(f: String => Option[LightUser]) extends (String => Option[LightUser]) {
    def apply(u: String) = f(u)
  }

  final class IsBotSync(f: String => Boolean) extends (String => Boolean) {
    def apply(userId: String) = f(userId)
  }
}
