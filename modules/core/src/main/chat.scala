package lila.core
package chat

import play.api.libs.json.JsObject

import lila.core.id.ChatId
import lila.core.shutup.PublicSource
import lila.core.userId.*

case class ChatLine(chatId: ChatId, line: Line, json: JsObject)
case class OnTimeout(chatId: ChatId, userId: UserId)
case class OnReinstate(chatId: ChatId, userId: UserId)

trait Line:
  def text: String
  def author: String
  def deleted: Boolean
  def isSystem    = author == UserName.lichess.value
  def isHuman     = !isSystem
  def humanAuthor = isHuman.option(author)
  def troll: Boolean
  def flair: Boolean
  def userIdMaybe: Option[UserId]

enum BusChan:
  val chan = s"chat:$toString"
  case round
  case tournament
  case simul
  case study
  case team
  case swiss
  case global
object BusChan:
  type Select = BusChan.type => BusChan

enum TimeoutReason(val key: String, val name: String):
  lazy val shortName = name.split(';').lift(0) | name
  case PublicShaming extends TimeoutReason("shaming", "public shaming; please use lichess.org/report")
  case Insult
      extends TimeoutReason("insult", "disrespecting other players; see lichess.org/page/chat-etiquette")
  case Spam  extends TimeoutReason("spam", "spamming the chat; see lichess.org/page/chat-etiquette")
  case Other extends TimeoutReason("other", "inappropriate behavior; see lichess.org/page/chat-etiquette")
object TimeoutReason:
  val all                = values.toList
  def apply(key: String) = all.find(_.key == key)

enum TimeoutScope:
  case Local, Global

trait ChatApi:
  def exists(chatId: ChatId): Fu[Boolean]
  def write(
      chatId: ChatId,
      userId: UserId,
      text: String,
      publicSource: Option[PublicSource],
      busChan: BusChan.Select,
      persist: Boolean = true
  ): Funit
  def volatile(chatId: ChatId, text: String, busChan: BusChan.Select): Unit
  def system(chatId: ChatId, text: String, busChan: BusChan.Select): Funit
  def timeout(
      chatId: ChatId,
      userId: UserId,
      reason: TimeoutReason,
      scope: TimeoutScope,
      text: String,
      busChan: BusChan.Select
  )(using MyId): Funit
