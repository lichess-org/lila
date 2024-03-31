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
