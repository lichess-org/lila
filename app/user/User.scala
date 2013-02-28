package lila.app
package user

import com.novus.salat.annotations.Key
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

case class User(
    @Key("_id") id: String,
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
    isChatBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    settings: Map[String, String] = Map.empty,
    bio: Option[String] = None,
    engine: Boolean = false,
    toints: Int = 0,
    createdAt: DateTime) {

  def muted = isChatBan

  def canChat = 
    !isChatBan && 
    nbGames >= 3 &&
    createdAt < (DateTime.now - 3.hours)

  def canMessage = !muted

  def canTeam = 
    !isChatBan && 
    nbGames >= 3 &&
    createdAt < (DateTime.now - 1.day)

  def disabled = !enabled

  def usernameWithElo = "%s (%d)".format(username, elo)

  def setting(name: String): Option[Any] = settings get name

  def nonEmptyBio = bio filter ("" !=)

  def hasGames = nbGames > 0
}

object User {

  val STARTING_ELO = 1200

  val anonymous = "Anonymous"
}
