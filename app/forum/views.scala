package lila
package forum

case class CategView(
    categ: Categ,
    lastPost: Option[(Topic, Post, Int)]) {

  def slug = categ.slug
  def name = categ.name
  def desc = categ.desc
  def nbTopics = categ.nbTopics
  def nbPosts = categ.nbPosts
}

case class TopicView(
    categ: Categ,
    topic: Topic,
    lastPost: Option[Post],
    lastPage: Int) {

  def id = topic.id
  def slug = topic.slug
  def name = topic.name
  def views = topic.views
  def createdAt = topic.createdAt
  def nbPosts = topic.nbPosts
}

case class PostView(
  post: Post,
  topic: Topic,
  categ: Categ,
  topicLastPage: Int)

case class PostLiteView(post: Post, topic: Topic, topicLastPage: Int)
