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

  private type UserID = String

  implicit val lightUserWrites = OWrites[LightUser] { u =>
    Json
      .obj(
        "id"   -> u.id,
        "name" -> u.name
      )
      .add("title" -> u.title)
      .add("patron" -> u.isPatron)
  }

  def fallback(userId: UserID) = LightUser(
    id = userId,
    name = userId,
    title = None,
    isPatron = false
  )

  final class Getter(f: UserID => Fu[Option[LightUser]]) extends (UserID => Fu[Option[LightUser]]) {
    def apply(u: UserID) = f(u)
  }

  final class GetterSync(f: UserID => Option[LightUser]) extends (UserID => Option[LightUser]) {
    def apply(u: UserID) = f(u)
  }

  final class IsBotSync(f: UserID => Boolean) extends (UserID => Boolean) {
    def apply(userId: UserID) = f(userId)
  }
}
