package lila.chat

import akka.actor._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Input
import play.api.libs.json._

import lila.common.Bus
import lila.socket.actorApi.{ SocketEnter, SocketLeave }
import lila.socket.SocketMember

private[chat] final class LichessBot extends Actor {

  val chat = context.parent

  def receive = {
    case line: Line ⇒ line.text match {

      case text if text.startsWith("help") ⇒ chat ! Line.system(
        chan = line.chan,
        text = "You want help?",
        to = line.userId.some)

      case text ⇒ chat ! Line.system(
        chan = line.chan,
        text = s"Hi @${line.username}, I did not understand that.",
        to = line.userId.some)
    }
  }
}
