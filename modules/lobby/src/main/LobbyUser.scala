package lila.lobby

import lila.user.User

private[lobby] case class LobbyUser(
  id: String,
  username: String,
  lame: Boolean,
  ratingMap: Map[String, Int],
  blocking: Set[String]
)

private[lobby] object LobbyUser {

  def make(user: User, blocking: Set[User.ID]) = LobbyUser(
    id = user.id,
    username = user.username,
    lame = user.lame,
    ratingMap = user.perfs.ratingMap,
    blocking = blocking
  )
}
