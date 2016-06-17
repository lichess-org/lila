package lila.chat
package actorApi

case class UserTalk(chatId: String, userId: String, text: String, public: Boolean = true)
case class PlayerTalk(chatId: String, white: Boolean, text: String)
case class SystemTalk(chatId: String, text: String)
case class ChatLine(chatId: String, line: Line)
case class Timeout(chatId: String, modId: String, userId: String, reason: ChatTimeout.Reason)

case class OnTimeout(username: String)
case class OnReinstate(userId: String)
