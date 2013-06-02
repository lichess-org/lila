package lila.user

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class User(
    id: String,
    username: String,
    elo: Int,
    count: Count,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    settings: Map[String, String] = Map.empty,
    bio: Option[String] = None,
    engine: Boolean = false,
    toints: Int = 0,
    createdAt: DateTime,
    seenAt: Option[DateTime]) extends Ordered[User] {

  def compare(other: User) = id compare other.id

  def is(other: User) = id == other.id

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithElo = "%s (%d)".format(username, elo)

  def setting(name: String): Option[Any] = settings get name

  def nonEmptyBio = bio filter ("" !=)

  def hasGames = count.game > 0
  
  def countRated = count.rated
}

object User {

  val STARTING_ELO = 1200

  val anonymous = "Anonymous"

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def countTube = Count.tube

  private[user] lazy val tube = Tube[User](
    (__.json update (
      merge(defaults) andThen readDate('createdAt) andThen readDateOpt('seenAt)
    )) andThen Json.reads[User],
    Json.writes[User] andThen (__.json update writeDate('createdAt)) andThen (__.json update writeDateOpt('seenAt))
  )

  def normalize(username: String) = username.toLowerCase

  private def defaults = Json.obj(
    "troll" -> false,
    "ipBan" -> false,
    "settings" -> Json.obj(),
    "engine" -> false,
    "toints" -> 0,
    "roles" -> Json.arr(),
    "seenAt" -> none[DateTime])
}
