package lila
package user

import com.novus.salat.annotations.Key
import com.mongodb.casbah.Imports.ObjectId

case class User(
    @Key("_id") id: ObjectId,
    username: String,
    isOnline: Boolean,
    elo: Int,
    nbGames: Int,
    nbRatedGames: Int,
    isChatBan: Boolean = false,
    enabled: Boolean = true,
    roles: List[String],
    password: String,
    salt: String,
    settings: Map[String, String] = Map.empty) {

  def usernameCanonical = username.toLowerCase

  def disabled = !enabled
  
  def usernameWithElo = "%s (%d)".format(username, elo)

  def setting(name: String): Option[Any] = settings get name
}

object User {

  val STARTING_ELO = 1200
}
