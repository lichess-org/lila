package lila
package user

import com.novus.salat.annotations.Key
import com.mongodb.casbah.Imports.ObjectId
import org.joda.time.DateTime

case class User(
    @Key("_id") id: ObjectId,
    username: String,
    elo: Int,
    nbGames: Int,
    nbRatedGames: Int,
    isChatBan: Boolean = false,
    enabled: Boolean,
    roles: List[String],
    settings: Map[String, String] = Map.empty,
    bio: Option[String] = None,
    engine: Boolean = false) {

  def usernameCanonical = username.toLowerCase

  def disabled = !enabled
  
  def usernameWithElo = "%s (%d)".format(username, elo)

  def setting(name: String): Option[Any] = settings get name

  def nonEmptyBio = bio filter ("" !=)

  def hasGames = nbGames > 0

  def idString = id.toString
}

object User {

  val STARTING_ELO = 1200

  val anonymous = "Anonymous"

  // the password is hashed
  def apply(username: String): User = User(
    id = new ObjectId,
    username = username,
    elo = STARTING_ELO,
    nbGames = 0,
    nbRatedGames = 0,
    isChatBan = false,
    enabled = true,
    roles = Nil,
    settings = Map.empty,
    bio = none,
    engine = false)
}
