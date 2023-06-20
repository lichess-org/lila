package lila.chat

export lila.Lila.{ *, given }

private val logger = lila.log("chat")

case class ChatLine(chatId: ChatId, line: Line)
case class RoundLine(line: Line, watcher: Boolean)
case class Timeout(chatId: ChatId, mod: UserId, userId: UserId, reason: ChatTimeout.Reason, local: Boolean)

case class OnTimeout(chatId: ChatId, userId: UserId)
case class OnReinstate(chatId: ChatId, userId: UserId)
