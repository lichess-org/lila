package lila.chat

import akka.actor._
import chess.Color

import lila.hub.actorApi.chat._

private[chat] final class FrontActor(api: ChatApi) extends Actor {

  private val bus = context.system.lilaBus

  bus.subscribe(self, 'chatIn)

  def receive = {

    case UserTalk(chatId, userId, text)  ⇒ api.userChat.write(chatId, userId, text) foreach publish(chatId)

    case PlayerTalk(chatId, color, text) ⇒ api.playerChat.write(chatId, Color(color), text) foreach publish(chatId)

    case SystemTalk(chatId, text)        ⇒ api.userChat.system(chatId, text) foreach publish(chatId)

    case msg: NotifyLine                 ⇒ bus.publish(msg, 'chatOut)
  }

  def publish(chatId: String)(lineOption: Option[Line]) {
    lineOption foreach { line ⇒
      self ! NotifyLine(chatId, Line toJson line)
    }
  }
}
