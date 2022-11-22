package lila.chat
package actorApi

case class ChatLine(chatId: ChatId, line: Line)
case class RoundLine(line: Line, watcher: Boolean)
case class Timeout(chatId: ChatId, mod: String, userId: String, reason: ChatTimeout.Reason, local: Boolean)

case class OnTimeout(chatId: ChatId, userId: String)
case class OnReinstate(chatId: ChatId, userId: String)
