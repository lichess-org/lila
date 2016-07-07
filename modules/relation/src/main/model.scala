package lila.relation

import lila.common.LightUser
import reactivemongo.bson._

case class Follower(u1: String) {
  def userId = u1
}

case class Followed(u2: String) {
  def userId = u2
}

case class Blocked(u2: String) {
  def userId = u2
}

private[relation] case class FriendEntering(user: LightUser, isPlaying: Boolean)

object BSONHandlers {

  private[relation] implicit val followerBSONHandler = Macros.handler[Follower]
  private[relation] implicit val followedBSONHandler = Macros.handler[Followed]
  private[relation] implicit val blockedBSONHandler = Macros.handler[Blocked]
}
