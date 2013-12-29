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

  val tutorialLines = """
_______________________ lichess chat _______________________
The text input at the bottom can be used to enter commands.
Commands start with a forward slash (/).
For instance, try and send the message /help to see available commands.
""".lines.toList filter (_.nonEmpty)

  val helpLines = """
___________________________ help ___________________________
/help                   display this message
/close                  close the chat
""".lines.toList filter (_.nonEmpty)

  def receive = {
    case line: Line ⇒ line.text match {

      case "help tutorial" ⇒ tutorialLines foreach { t ⇒
        chat ! Line.system(
          chan = line.chan,
          text = t,
          to = line.userId.some)
      }

      case "help" ⇒ helpLines foreach { t ⇒
        chat ! Line.system(
          chan = line.chan,
          text = t,
          to = line.userId.some)
      }

      case text ⇒ chat ! Line.system(
        chan = line.chan,
        text = s"Command not found. Type /help for the list of available commands.",
        to = line.userId.some)
    }
  }
}
