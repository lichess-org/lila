package lila.round
package actorApi

import chess.Color
import lila.socket.SocketMember
import lila.game.{ Game, Event }
import lila.user.User

sealed trait Member extends SocketMember {

  val color: Color
  val owner: Boolean
  val troll: Boolean

  def watcher = !owner
}

object Member {
  def apply(
    channel: JsChannel,
    user: Option[User],
    color: Color,
    owner: Boolean): Member = {
    val userId = user map (_.id)
    val troll = user.zmap(_.troll)
    owner.fold(
      Owner(channel, userId, color, troll),
      Watcher(channel, userId, color, troll))
  }
}

case class Owner(
    channel: JsChannel,
    userId: Option[String],
    color: Color,
    troll: Boolean) extends Member {

  val owner = true
}

case class Watcher(
    channel: JsChannel,
    userId: Option[String],
    color: Color,
    troll: Boolean) extends Member {

  val owner = false
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int,
  color: Color,
  owner: Boolean)
case class Connected(enumerator: JsEnumerator, member: Member)
case class GetEventsSince(version: Int)
case class MaybeEvents(events: Option[List[VersionedEvent]])
case class AddEvents(events: List[Event])
case object ClockSync
case class IsConnectedOnGame(gameId: String, color: Color)
case class IsGone(gameId: String, color: Color)
case object AnalysisAvailable
case class Ack(uid: String)
