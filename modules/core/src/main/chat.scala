package lila.core
package chat

object panic:
  type IsAllowed = UserId => (UserId => Fu[Option[user.User]]) => Fu[Boolean]

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

trait ChatApi:
  def volatile(chatId: ChatId, text: String, busChan: BusChan.Select): Unit
