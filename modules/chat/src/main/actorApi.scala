package lila.chat
package actorApi

case class ChatLine(chatId: Chat.Id, line: Line)
case class Timeout(chatId: Chat.Id, mod: String, userId: String, reason: ChatTimeout.Reason, local: Boolean)

case class OnTimeout(userId: String)
case class OnReinstate(userId: String)
