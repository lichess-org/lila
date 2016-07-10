package lila.chat

import akka.actor._
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.socket.{ Handler, SocketMember, Historical }

object Socket {

  def in(
    chatId: String,
    member: SocketMember,
    socket: ActorRef,
    chat: ActorSelection): Handler.Controller = {

    case ("talk", o) => for {
      text <- o str "d"
      userId <- member.userId
    } chat ! actorApi.UserTalk(chatId, userId, text)

    case ("timeout", o) => for {
      data ← o obj "d"
      modId <- member.userId
      userId <- data.str("userId")
      reason <- data.str("reason") flatMap ChatTimeout.Reason.apply
    } chat ! actorApi.Timeout(chatId, modId, userId, reason)
  }

  type Send = (String, JsValue, Boolean) => Unit

  def out(send: Send): Actor.Receive = {

    case actorApi.ChatLine(_, line) => line match {
      case line: UserLine => send("message", JsonView(line), line.troll)
      case _              =>
    }

    case actorApi.OnTimeout(username) => send("chat_timeout", JsString(username), false)

    case actorApi.OnReinstate(userId) => send("chat_reinstate", JsString(userId), false)
  }
}
