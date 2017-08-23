package lila.forum

import org.joda.time.DateTime

case class CategView(
    categ: Categ,
    lastPost: Option[(Topic, Post, Int)],
    troll: Boolean
) {

  def nbTopics = categ nbTopics troll
  def nbPosts = categ nbPosts troll
  def lastPostId = categ lastPostId troll
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
    troll: Boolean
) {

  def updatedAt = topic updatedAt troll
  def nbPosts = topic nbPosts troll
  def nbReplies = topic nbReplies troll
  def lastPostId = topic lastPostId troll
  def lastPostUserId = lastPost.flatMap(_.userId)

  def id = topic.id
  def slug = topic.slug
  def name = topic.name
  def views = topic.views
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
