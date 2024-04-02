package lila.core
package mod

import lila.core.user.MyId

trait LogApi:
  def toggleStickyTopic(categ: ForumCategId, topicSlug: String, sticky: Boolean)(using MyId): Funit
  def toggleCloseTopic(categ: ForumCategId, topicSlug: String, closed: Boolean)(using MyId): Funit
  def postOrEditAsAnonMod(
      categ: ForumCategId,
      topic: String,
      postId: ForumPostId,
      text: String,
      edit: Boolean
  )(using MyId): Funit

trait ModApi:
  def autoMark(suspectId: report.SuspectId, note: String)(using MyId): Funit

case class MarkCheater(userId: UserId, value: Boolean)
case class MarkBooster(userId: UserId)
case class ChatTimeout(mod: UserId, user: UserId, reason: String, text: String)
case class Shadowban(user: UserId, value: Boolean)
case class KickFromRankings(userId: UserId)
case class AutoWarning(userId: UserId, subject: String)
case class Impersonate(userId: UserId, by: Option[UserId])
case class SelfReportMark(userId: UserId, name: String)
