package lila.user

import scala._

case class Profile(
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    bio: Option[String] = None,
    country: Option[String] = None) {

  def realName = (firstName |@| lastName) apply { _ + " " + _ }

  def nonEmptyBio = bio filter (_.nonEmpty)

  def nonEmpty = List(
    firstName, lastName, bio, country
  ).flatten.nonEmpty option this
}

object Profile {

  val default = Profile()

  import lila.db.Tube
  import play.api.libs.json._

  private[user] lazy val tube = Tube[Profile](
    Json.reads[Profile],
    Json.writes[Profile])
}
