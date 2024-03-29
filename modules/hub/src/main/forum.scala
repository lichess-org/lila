package lila.hub
package forum

case class CreatePost(post: ForumPostMini)
case class RemovePost(id: ForumPostId)
case class RemovePosts(ids: List[ForumPostId])

trait ForumPost:
  val id: ForumPostId
  val text: String

case class ForumPostMini(
    id: ForumPostId,
    topicId: ForumTopicId,
    userId: Option[UserId],
    text: String,
    troll: Boolean,
    createdAt: Instant
)
case class ForumTopicMini(id: ForumTopicId, name: String, slug: String, categId: ForumCategId):
  def possibleTeamId: Option[TeamId] = ForumCateg.toTeamId(categId)

case class ForumPostMiniView(post: ForumPostMini, topic: ForumTopicMini)

object ForumCateg:
  def isTeamSlug(id: ForumCategId)               = id.value.startsWith("team-")
  def toTeamId(id: ForumCategId): Option[TeamId] = isTeamSlug(id).option(TeamId(id.value.drop(5)))

trait ForumPostApi:
  def getPost(postId: ForumPostId): Fu[Option[ForumPost]]
  def miniViews(postIds: Seq[ForumPostId]): Fu[Seq[ForumPostMiniView]]
