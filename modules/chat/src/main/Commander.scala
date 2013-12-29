package lila.chat

import akka.actor._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Input
import play.api.libs.json._

import actorApi._
import lila.common.Bus
import lila.socket.actorApi.{ SocketEnter, SocketLeave }
import lila.socket.SocketMember

private[chat] final class Commander extends Actor {

  val chat = context.parent

  def receive = {
    case Command(chanOption, member, text) ⇒ text.split(' ').toList match {

      case "help" :: "tutorial" :: _ ⇒ chanOption foreach { chan ⇒
        tutorialLines foreach replyTo(chan, member)
      }

      case "help" :: _ ⇒ chanOption foreach { chan ⇒
        helpLines foreach replyTo(chan, member)
      }

      case "open" :: _              ⇒ chat ! SetOpen(member, true)
      case "close" :: _             ⇒ chat ! SetOpen(member, false)

      case "query" :: username :: _ ⇒ chat ! Query(member, username.toLowerCase)

      case words ⇒ chanOption foreach { chan ⇒
        replyTo(chan, member) {
          s"Command not found. Type /help for the list of available commands."
        }
      }
    }
  }

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

  private def replyTo(chan: Chan, member: ChatMember)(text: String) {
    chat ! Tell(member.uid, Line.system(chan = chan, text = text))
  }
}
