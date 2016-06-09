package lila.chat
package actorApi

import akka.actor.ActorRef

case class UserTalk(chatId: String, userId: String, text: String, replyTo: ActorRef, public: Boolean = true)
case class PlayerTalk(chatId: String, white: Boolean, text: String, replyTo: ActorRef)
case class SystemTalk(chatId: String, text: String, replyTo: ActorRef)
case class ChatLine(chatId: String, line: Line)
case class TempBan(chatId: String, modId: String, userId: String)
