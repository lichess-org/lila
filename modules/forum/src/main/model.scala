package lila.forum

import lila.user.User

case class CategView(
    categ: ForumCateg,
    lastPost: Option[(ForumTopic, ForumPost, Int)],
    forUser: Option[User]
):

  def nbTopics       = categ nbTopics forUser
  def nbPosts        = categ nbPosts forUser
  def lastPostId     = categ lastPostId forUser
  def lastPostUserId = lastPost.map(_._2).flatMap(_.userId)

  def slug = categ.slug
  def name = categ.name
  def desc = categ.desc

case class TopicView(
    categ: ForumCateg,
    topic: ForumTopic,
    lastPost: Option[ForumPost],
    lastPage: Int,
    forUser: Option[User]
):

  def updatedAt      = topic updatedAt forUser
  def nbPosts        = topic nbPosts forUser
  def nbReplies      = topic nbReplies forUser
  def lastPostId     = topic lastPostId forUser
  def lastPostUserId = lastPost.flatMap(_.userId)

  def id        = topic.id
  def slug      = topic.slug
  def name      = topic.name
  def createdAt = topic.createdAt

case class PostView(
    post: ForumPost,
    topic: ForumTopic,
    categ: ForumCateg
):

  def show         = post.showUserIdOrAuthor + " @ " + topic.name + " - " + post.text.take(80)
  def logFormatted = "%s / %s#%s / %s".format(categ.name, topic.name, post.number, post.text)

object PostView:
  case class WithReadPerm(view: PostView, canRead: Boolean)

case class PostLiteView(post: ForumPost, topic: ForumTopic)

case class MiniForumPost(
    isTeam: Boolean,
    postId: ForumPostId,
    topicName: String,
    userId: Option[UserId],
    text: String,
    createdAt: Instant
)

case class PostUrlData(categ: ForumCategId, topicSlug: String, page: Int, number: Int)

enum Filter:
  case Safe
  case SafeAnd(userId: UserId)
  case Unsafe

case class InsertPost(post: ForumPost)
case class RemovePost(id: ForumPostId)
case class RemovePosts(ids: List[ForumPostId])
case class CreatePost(post: ForumPost)
