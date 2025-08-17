package lila.core
package mod

import lila.core.chat.TimeoutReason
import lila.core.id.{ ForumCategId, ForumPostId, ForumTopicSlug, GameFullId }
import lila.core.userId.*

trait LogApi:
  def toggleStickyTopic(categ: ForumCategId, slug: ForumTopicSlug, sticky: Boolean)(using MyId): Funit
  def toggleCloseTopic(categ: ForumCategId, slug: ForumTopicSlug, closed: Boolean)(using MyId): Funit
  def postOrEditAsAnonMod(
      categ: ForumCategId,
      topic: ForumTopicSlug,
      postId: ForumPostId,
      text: String,
      edit: Boolean
  )(using MyId): Funit

trait ModApi:
  def autoMark(suspectId: report.SuspectId, note: String)(using MyId): Funit

case class MarkCheater(userId: UserId, value: Boolean)
case class MarkBooster(userId: UserId)
case class ChatTimeout(mod: UserId, user: UserId, reason: TimeoutReason, text: String)
case class Shadowban(user: UserId, value: Boolean)
case class KickFromRankings(userId: UserId)
case class AutoWarning(userId: UserId, subject: String)
case class Impersonate(modId: ModId, userId: UserId, v: Boolean)
case class SelfReportMark(userId: UserId, name: String, gameId: GameFullId)
case class BoardApiMark(userId: UserId, name: String)
case class LoginWithWeakPassword(userId: UserId)
case class LoginWithBlankedPassword(userId: UserId)
