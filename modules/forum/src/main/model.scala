package lila.forum

import org.joda.time.DateTime

import lila.user.User

case class CategView(
    categ: Categ,
    lastPost: Option[(Topic, Post, Int)],
    forUser: Option[User]
) {

  def nbTopics       = categ nbTopics forUser
  def nbPosts        = categ nbPosts forUser
  def lastPostId     = categ lastPostId forUser
  def lastPostUserId = lastPost.map(_._2).flatMap(_.userId)

  def slug = categ.slug
  def name = categ.name
  def desc = categ.desc
}

case class TopicView(
    categ: Categ,
    topic: Topic,
    lastPost: Option[Post],
    lastPage: Int,
    forUser: Option[User]
) {

  def updatedAt      = topic updatedAt forUser
  def nbPosts        = topic nbPosts forUser
  def nbReplies      = topic nbReplies forUser
  def lastPostId     = topic lastPostId forUser
  def lastPostUserId = lastPost.flatMap(_.userId)

  def id        = topic.id
  def slug      = topic.slug
  def name      = topic.name
  def views     = topic.views
  def createdAt = topic.createdAt
}

case class PostView(
    post: Post,
    topic: Topic,
    categ: Categ,
    topicLastPage: Int
) {

  def show = post.showUserIdOrAuthor + " @ " + topic.name + " - " + post.text.take(80)
}

case class PostLiteView(post: Post, topic: Topic)

case class MiniForumPost(
    isTeam: Boolean,
    postId: String,
    topicName: String,
    userId: Option[String],
    text: String,
    createdAt: DateTime
)

case class PostUrlData(categ: String, topic: String, page: Int, number: Int)

object Filter {
  sealed trait Filter
  case object Safe                   extends Filter
  case class SafeAnd(userId: String) extends Filter
  case object Unsafe                 extends Filter
}
