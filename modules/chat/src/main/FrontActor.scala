package lidraughts.chat

import akka.actor._
import draughts.Color

import actorApi._

private[chat] final class FrontActor(
    api: ChatApi,
    palantir: Palantir
) extends Actor {

  def receive = {

    case UserTalk(chatId, userId, text, source) => api.userChat.write(chatId, userId, text, source)

    case PlayerTalk(chatId, color, text) => api.playerChat.write(chatId, Color(color), text)

    case SystemTalk(chatId, text) => api.userChat.system(chatId, text)

    case Timeout(chatId, modId, userId, reason, local) => api.userChat.timeout(chatId, modId, userId, reason, local)

    case Remove(chatId) => api remove chatId

    case RemoveAll(chatIds) => api removeAll chatIds

    case Palantir.Ping(chatId, userId, sri) => palantir.ping(chatId, userId, sri)
  }
}
