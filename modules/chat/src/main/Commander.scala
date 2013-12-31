package lila.chat

import scala.concurrent.duration._

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

      case "help" :: "tutorial" :: _ ⇒ chat ! Tell(member, tutorialLines)
      case "help" :: _               ⇒ chat ! Tell(member, helpLines)

      case "open" :: _               ⇒ chat ! SetOpen(member, true)
      case "close" :: _              ⇒ chat ! SetOpen(member, false)

      case "query" :: username :: _  ⇒ chat ! Query(member, username.toLowerCase)

      case "join" :: chanName :: _ ⇒ Chan parse chanName match {
        case Some(chan) ⇒ chat ! Join(member, chan)
        case None       ⇒ chat ! Tell(member, s"The channel $chanName does not exist")
      }
      case "show" :: chanName :: _ ⇒ Chan parse chanName foreach { chan ⇒
        chat ! Activate(member, chan)
      }
      case "hide" :: chanName :: _ ⇒ Chan parse chanName foreach { chan ⇒
        chat ! DeActivate(member, chan)
      }

      case words ⇒ chat ! Tell(member, s"Command not found. Type /help for the list of available commands.")
    }
  }

  import org.apache.commons.lang3.StringEscapeUtils.escapeXml

  val tutorialLines = "<pre>" + escapeXml("""
_______________________ lichess chat _______________________
The text input at the bottom can be used to enter commands.
Commands start with a forward slash (/).
For instance, try and send the message /help to see available commands.
""") + "</pre>"

  val helpLines = "<pre>" + escapeXml("""
___________________________ help ___________________________
/help                   display this message
/join <chan>            enter a chat room. Ex: /join en
/query <friend>         start a private chat with a friend
/close                  close the chat
""") + "</pre>"
}
