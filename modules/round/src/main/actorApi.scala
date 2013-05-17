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
case class IsConnectedOnGame(gameId: String, color: Color)
case class IsGone(gameId: String, color: Color)
case object AnalysisAvailable
case class Ack(uid: String)

package round {

  case class Play(
    color: Color,
    orig: String,
    dest: String,
    prom: Option[String] = None,
    blur: Boolean = false,
    lag: Int = 0)

  case class Abort(fullId: String)
  case class Resign(fullId: String, force: Boolean)
  case class Outoftime(color: Color)
  case class DrawClaim(fullId: String)
  case class DrawAccept(fullId: String)
  case class DrawOffer(fullId: String)
  case class DrawCancel(fullId: String)
  case class DrawDecline(fullId: String)
  case class RematchCancel(fullId: String)
  case class RematchDecline(fullId: String)
  case class TakebackAccept(fullId: String)
  case class TakebackOffer(fullId: String)
  case class TakebackCancel(fullId: String)
  case class TakebackDecline(fullId: String)
  case class Moretime(color: Color)
}
