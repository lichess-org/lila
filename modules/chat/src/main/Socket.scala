package lidraughts.chat

import akka.actor._
import play.api.libs.json._

import lidraughts.hub.actorApi.shutup.PublicSource
import lidraughts.socket.{ Handler, SocketMember }
import lidraughts.user.User

object Socket {

  def in(
    chatId: Chat.Id,
    member: SocketMember,
    chat: ActorSelection,
    publicSource: Option[PublicSource],
    canTimeout: Option[User.ID => Fu[Boolean]] = None
  ): Handler.Controller = {

    case ("talk", o) => for {
      text <- o str "d"
      userId <- member.userId
    } chat ! actorApi.UserTalk(chatId, userId, text, publicSource)

    case ("timeout", o) => for {
      data ← o obj "d"
      modId <- member.userId
      userId <- data.str("userId") map lidraughts.user.User.normalize
      reason <- data.str("reason") flatMap ChatTimeout.Reason.apply
    } canTimeout.??(_(userId)) foreach { localTimeout =>
      chat ! actorApi.Timeout(chatId, modId, userId, reason, local = localTimeout)
    }

    case ("palantirPing", o) =>
      member.userId foreach { userId =>
        chat ! Palantir.Ping(chatId, userId, member)
      }
  }

  type Send = (String, JsValue, Boolean) => Unit

  def out(send: Send): Actor.Receive = {

    case actorApi.ChatLine(_, line) => line match {
      case line: UserLine => send("message", JsonView(line), line.troll)
      case _ =>
    }

    case actorApi.OnTimeout(username) => send("chat_timeout", JsString(username), false)

    case actorApi.OnReinstate(userId) => send("chat_reinstate", JsString(userId), false)
  }
}
