package lila.chat

import scala.concurrent.duration._

import akka.actor._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Input
import play.api.libs.json._

import actorApi._
import lila.common.Bus
import lila.security.{ Permission, Granter }
import lila.socket.actorApi.{ SocketEnter, SocketLeave }
import lila.socket.SocketMember
import lila.user.{ User, UserRepo }

private[chat] final class Commander(
    modApi: lila.mod.ModApi) extends Actor {

  val chat = context.parent

  def receive = {
    case Command(chanOption, member, text) ⇒ text.split(' ').toList match {

      case "help" :: "tutorial" :: _ ⇒ flash(member, tutorial)
      case "help" :: _               ⇒ flash(member, help)

      case "open" :: _               ⇒ chat ! SetOpen(member, true)
      case "close" :: _              ⇒ chat ! SetOpen(member, false)

      case "query" :: username :: _  ⇒ chat ! Query(member, username.toLowerCase)

      case "join" :: chanName :: _ ⇒ Chan parse chanName match {
        case Some(chan) ⇒ chat ! Join(member, chan)
        case None       ⇒ flash(member, s"The channel $chanName does not exist.")
      }
      case "show" :: chanName :: _ ⇒ Chan parse chanName foreach { chan ⇒
        chat ! Activate(member, chan)
      }
      case "hide" :: chanName :: _ ⇒ Chan parse chanName foreach { chan ⇒
        chat ! DeActivate(member, chan)
      }

      case ("rematch" | "resign" | "abort" | "takeback") :: _ ⇒ gameOnlyCommand(member)
      case MoveRegex(orig, dest) :: _                         ⇒ gameOnlyCommand(member)

      case "troll" :: username :: _ ⇒ Secure(member, _.MarkTroll) { me ⇒
        modApi.troll(me.id, username) foreach { troll ⇒
          flash(member, s"User $username is ${troll.fold("now", "no longer")} a troll.")
        }
      }

      case words ⇒ flash(member, s"Command not found: ${words mkString " "}. Type /help for the list of available commands.")
    }
  }

  private val MoveRegex = """^([a-h][1-8])([a-h][1-8])$""".r
  private val PovIdRegex = """^(\w{12})$""".r

  private def flash(member: ChatMember, text: String) {
    chat ! Flash(member, text)
  }

  private def gameOnlyCommand(member: ChatMember) {
    flash(member, "Use this command when playing a game.")
  }

  private def Secure(member: ChatMember, perm: Permission.type ⇒ Permission)(f: User ⇒ Unit) {
    UserRepo byId member.userId foreach {
      case Some(u) if Granter(perm)(u) ⇒ f(u)
      case _                           ⇒ flash(member, s"Permission denied. Well tried, tho.")
    }
  }

  import org.apache.commons.lang3.StringEscapeUtils.escapeXml

  val tutorial = "<pre>" + escapeXml("""
_______________________ lichess chat _______________________
The text input at the bottom can be used to command lichess!
Commands start with a forward slash (/).
For instance, try and send /help to see available commands.
""") + "</pre>"

  val help = "<pre>" + escapeXml("""
_______________________ chat commands ______________________
/help                   display this message
/join <chan>            enter a chat room. Ex: /join en
/query <friend>         start a private chat with a friend
_______________________ user commands ______________________
/msg <user>             send a message to a user
/report <user>          report a user to the moderators
_______________________ game commands ______________________
/e2e4                   move the piece on e2 to e4
/abort, /resign, /takeback, /rematch
""") + "</pre>"
}
