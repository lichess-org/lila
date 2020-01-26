package lila.relation

import lila.common.LightUser
import reactivemongo.api.bson._

case class Follower(u1: String) {
  def userId = u1
}

case class Followed(u2: String) {
  def userId = u2
}

case class Blocked(u2: String) {
  def userId = u2
}

case class Related(
    user: lila.user.User,
    nbGames: Option[Int],
    followable: Boolean,
    relation: Option[Relation]
)

case class Relations(
    in: Option[Relation],
    out: Option[Relation]
)

private[relation] case class FriendEntering(user: LightUser, isPlaying: Boolean, isStudying: Boolean)

object BSONHandlers {

  implicit private[relation] val followerBSONHandler = Macros.handler[Follower]
  implicit private[relation] val followedBSONHandler = Macros.handler[Followed]
  implicit private[relation] val blockedBSONHandler  = Macros.handler[Blocked]
}
