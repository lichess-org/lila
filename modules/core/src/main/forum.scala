package lila.core
package forum

import reactivemongo.api.bson.Macros.Annotations.Key

case class CreatePost(post: ForumPostMini)
case class RemovePost(id: ForumPostId, by: Option[UserId], text: String, asAdmin: Boolean)(using
    val me: user.MyId
)
case class RemovePosts(ids: List[ForumPostId])
// case class PostCloseToggle(categ: ForumCategId, topicSlug: String, closed: Boolean)(using val me: user.MyId)
// erasing = blankng, still in db but with empty text
case class ErasePost(id: ForumPostId)
case class ErasePosts(ids: List[ForumPostId])

trait ForumPost:
  val id: ForumPostId
  val text: String

case class ForumPostMini(
    @Key("_id") id: ForumPostId,
    topicId: ForumTopicId,
    userId: Option[UserId],
    text: String,
    troll: Boolean,
    createdAt: Instant
)
case class ForumTopicMini(
    @Key("_id") id: ForumTopicId,
    name: String,
    slug: String,
    categId: ForumCategId
):
  def possibleTeamId: Option[TeamId] = ForumCateg.toTeamId(categId)

case class ForumPostMiniView(post: ForumPostMini, topic: ForumTopicMini)

object ForumCateg:
  def isTeamSlug(id: ForumCategId)               = id.value.startsWith("team-")
  def toTeamId(id: ForumCategId): Option[TeamId] = isTeamSlug(id).option(TeamId(id.value.drop(5)))

trait ForumPostApi:
  def getPost(postId: ForumPostId): Fu[Option[ForumPost]]
  def miniViews(postIds: List[ForumPostId]): Fu[List[ForumPostMiniView]]
  def toMiniView(post: ForumPostMini): Fu[Option[ForumPostMiniView]]
  def toMiniViews(posts: List[ForumPostMini]): Fu[List[ForumPostMiniView]]
  def nonGhostCursor: reactivemongo.akkastream.AkkaStreamCursor[ForumPostMini]
