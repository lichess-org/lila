package lila.chat

import akka.actor._
import chess.Color

import actorApi._
import lila.common.PimpedJson._

private[chat] final class FrontActor(api: ChatApi) extends Actor {

  def receive = {

    case UserTalk(chatId, userId, text, replyTo, public) =>
      api.userChat.write(chatId, userId, text, public) foreach publish(chatId, replyTo)

    case PlayerTalk(chatId, color, text, replyTo) =>
      api.playerChat.write(chatId, Color(color), text) foreach publish(chatId, replyTo)

    case SystemTalk(chatId, text, replyTo) =>
      api.userChat.system(chatId, text) foreach publish(chatId, replyTo)

    case Timeout(chatId, member, o) => for {
      data ← o obj "d"
      modId <- member.userId
      username <- data.str("username")
      reason <- data.str("reason") flatMap ChatTimeout.Reason.apply
    } api.userChat.timeout(chatId, modId, username, reason)
  }

  def publish(chatId: String, replyTo: ActorRef)(lineOption: Option[Line]) {
    lineOption foreach { line =>
      replyTo ! ChatLine(chatId, line)
    }
  }
}
