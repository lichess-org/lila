package lila.chat

import akka.actor._

import lila.common.PimpedJson._
import lila.socket.{ Handler, SocketMember }

object SocketHandler {

  def apply(
    chatId: String,
    member: SocketMember,
    socket: ActorRef,
    chat: ActorSelection): Handler.Controller = {

    case ("talk", o) => for {
      text <- o str "d"
      userId <- member.userId
    } chat ! lila.chat.actorApi.UserTalk(chatId, userId, text, socket)

    case ("timeout", o) => for {
      data ← o obj "d"
      modId <- member.userId
      userId <- data.str("userId")
      reason <- data.str("reason") flatMap ChatTimeout.Reason.apply
    } chat ! lila.chat.actorApi.Timeout(chatId, modId, userId, reason)
  }
}
