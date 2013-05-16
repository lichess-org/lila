package lila.user

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class User(
    id: String,
    username: String,
    elo: Int,
    nbGames: Int,
    nbRatedGames: Int,
    nbWins: Int,
    nbLosses: Int,
    nbDraws: Int,
    nbWinsH: Int, // only against human opponents
    nbLossesH: Int, // only against human opponents
    nbDrawsH: Int, // only against human opponents
    nbAi: Int,
    troll: Boolean = false,
    ipBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    settings: Map[String, String] = Map.empty,
    bio: Option[String] = None,
    engine: Boolean = false,
    toints: Int = 0,
    createdAt: DateTime) {

  def noTroll = !troll

  def canTeam = true

  def disabled = !enabled

  def usernameWithElo = "%s (%d)".format(username, elo)

  def setting(name: String): Option[Any] = settings get name

  def nonEmptyBio = bio filter ("" !=)

  def hasGames = nbGames > 0
}

object User {

  val STARTING_ELO = 1200

  val anonymous = "Anonymous"

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[user] lazy val tube = Tube[User](
    (__.json update (
      merge(defaults) andThen readDate('createdAt)
    )) andThen Json.reads[User],
    Json.writes[User] andThen (__.json update writeDate('createdAt))
  )

  def normalize(username: String) = username.toLowerCase

  private def defaults = Json.obj(
    "troll" -> false,
    "ipBan" -> false,
    "settings" -> Json.obj(),
    "engine" -> false,
    "toints" -> 0,
    "settings" -> Json.obj(),
    "roles" -> Json.arr())
}
