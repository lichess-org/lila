package lila.chat
package actorApi

case class ChatLine(chatId: Chat.Id, line: Line)
case class RoundLine(line: Line, watcher: Boolean)
case class Timeout(chatId: Chat.Id, mod: String, userId: String, reason: ChatTimeout.Reason, local: Boolean)

case class OnTimeout(chatId: Chat.Id, userId: String)
case class OnReinstate(chatId: Chat.Id, userId: String)
