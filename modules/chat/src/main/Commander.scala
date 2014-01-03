package lila.chat

import scala.concurrent.duration._

import akka.actor._
import org.apache.commons.lang3.StringEscapeUtils.escapeXml
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
    modApi: lila.mod.ModApi,
    namer: Namer,
    getTeamIds: String ⇒ Fu[List[String]]) extends Actor {

  private val chat = context.parent

  private val aliases = Map(
    "leave" -> "part")

  def receive = {
    case command@Command(chanOption, member, text) ⇒ text.split(' ').toList match {

      case aliased :: words if aliases.contains(aliased) ⇒ self ! command.copy(
        text = ((aliases.get(aliased) | aliased) :: words) mkString " ")

      case "help" :: "tutorial" :: Nil ⇒ flash(member, tutorial)
      case "help" :: "mod" :: Nil      ⇒ flash(member, modHelp)
      case "help" :: Nil               ⇒ flash(member, help)

      case "open" :: Nil               ⇒ chat ! SetOpen(member, true)
      case "close" :: Nil              ⇒ chat ! SetOpen(member, false)

      case "query" :: username :: Nil  ⇒ chat ! Query(member, username.toLowerCase)

      case "join" :: chanKey :: Nil ⇒ Chan parse chanKey match {
        case Some(chan@TeamChan(teamId)) ⇒ getTeamIds(member.userId) foreach {
          case teamIds if teamIds.contains(teamId) ⇒ chat ! Join(member, chan)
          case _                                   ⇒ flash(member, s"You are not a member of this team.")
        }
        case Some(chan) ⇒ chat ! Join(member, chan)
        case None       ⇒ flash(member, s"The channel $chanKey does not exist.")
      }

      case "show" :: chanKey :: Nil                ⇒ withChan(chanKey) { chan ⇒ chat ! Activate(member, chan) }
      case "hide" :: chanKey :: Nil                ⇒ withChan(chanKey) { chan ⇒ chat ! DeActivate(member, chan) }

      case "part" :: chanKey :: Nil                ⇒ withChan(chanKey) { chan ⇒ chat ! Part(member, chan) }
      case "part" :: Nil                           ⇒ member.head.mainChanKey flatMap Chan.parse foreach { chan ⇒ chat ! Part(member, chan) }

      case "say" :: words                          ⇒ chanOption foreach { chan ⇒ chat ! Say(chan, member, words mkString " ") }

      case escaped :: _ if escaped.startsWith("/") ⇒ self ! command.copy(text = "say " + text)

      case "names" :: Nil ⇒ chanOption foreach { chan ⇒
        userOf(member) foreach { user ⇒
          namer.chan(chan, user) foreach { named ⇒
            chat ! WithChanNicks(chan.key, { nicks ⇒
              flash(member, s"${nicks.size} users in ${named.name}: ${nicks.sorted.mkString(", ")}")
            })
          }
        }
      }

      case "height" :: height :: Nil                            ⇒ parseIntOption(height) foreach { h ⇒ chat ! SetHeight(member, h) }

      case ("rematch" | "resign" | "abort" | "takeback") :: Nil ⇒ gameOnlyCommand(member)
      case MoveRegex(orig, dest) :: Nil                         ⇒ gameOnlyCommand(member)

      case "troll" :: username :: Nil ⇒ Secure(member, _.MarkTroll) { me ⇒
        modApi.troll(me.id, username) foreach { troll ⇒
          flash(member, s"User $username is ${troll.fold("now", "no longer")} a troll.")
        }
      }

      case words ⇒ flash(member, s"Command not found: ${escapeXml(words mkString " ")}. Type /help for the list of available commands.")
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

  private def userOf(member: ChatMember): Fu[User] =
    UserRepo byId member.userId flatten s"No such user: $member.userId"

  private def withChan(key: String)(f: Chan ⇒ Unit) {
    Chan parse key foreach f
  }

  val tutorial = "<pre>" + escapeXml("""
_______________________ lichess chat _______________________
The text input at the bottom can be used to command lichess!
Commands start with a forward slash (/).
For instance, try and send /help to see available commands.
""") + "</pre>"

  val help = "<pre>" + escapeXml("""
_______________________ chat commands ______________________
/join <chan>            enter a chat room. Ex: /join en
/query <friend>         start a private chat with a friend
/part                   quit the current room
/names                  show the users connected to the current room
_______________________ user commands ______________________
/msg <user>             send a message to a user
/report <user>          report a user to the moderators
_______________________ game commands ______________________
/e2e4                   move the piece on e2 to e4
/abort, /resign, /takeback, /rematch
""") + "</pre>"

  val modHelp = "<pre>" + escapeXml("""
_______________________ mod commands _______________________
/troll <user>           toggle user troll status
""") + "</pre>"
}
