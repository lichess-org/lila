package lila.chat

import akka.actor._
import chess.Color

import actorApi._

private[chat] final class FrontActor(api: ChatApi) extends Actor {

  def receive = {

    case UserTalk(chatId, userId, text, public) => api.userChat.write(chatId, userId, text, public)

    case PlayerTalk(chatId, color, text)        => api.playerChat.write(chatId, Color(color), text)

    case SystemTalk(chatId, text)               => api.userChat.system(chatId, text)

    case Timeout(chatId, modId, userId, reason) => api.userChat.timeout(chatId, modId, userId, reason)
  }
}
