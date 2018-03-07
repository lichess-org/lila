package lila.chat

import akka.actor._
import play.api.libs.json._

import lila.socket.{ Handler, SocketMember }
import lila.hub.actorApi.shutup.Source

object Socket {

  def in(
    chatId: Chat.Id,
    member: SocketMember,
    socket: ActorRef,
    chat: ActorSelection,
    source: Source,
    canTimeout: Option[() => Fu[Boolean]] = None
  ): Handler.Controller = {

    case ("talk", o) => for {
      text <- o str "d"
      userId <- member.userId
    } chat ! actorApi.UserTalk(chatId, userId, text, source)

    case ("timeout", o) => for {
      data ← o obj "d"
      modId <- member.userId
      userId <- data.str("userId") map lila.user.User.normalize
      reason <- data.str("reason") flatMap ChatTimeout.Reason.apply
    } canTimeout.??(ct => ct()) foreach { localTimeout =>
      chat ! actorApi.Timeout(chatId, modId, userId, reason, local = localTimeout)
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
