package lila.core
package shutup

import lila.core.userId.UserId
import lila.core.chat.PublicSource

case class PublicLine(text: String, from: PublicSource, date: Instant)

trait ShutupApi:
  def publicText(userId: UserId, text: String, source: PublicSource): Funit
  def privateMessage(userId: UserId, toUserId: UserId, text: String): Funit
  def privateChat(chatId: String, userId: UserId, text: String): Funit
  def teamForumMessage(userId: UserId, text: String): Funit

trait TextAnalysis:
  val text: String
  val badWords: List[String]
  def dirty: Boolean
  def critical: Boolean

trait TextAnalyser:
  def apply(raw: String): TextAnalysis
  def containsLink(raw: String): Boolean
  def isCritical(raw: String): Boolean
