package lila
package forum

case class TopicView(
    categ: Categ,
    topic: Topic,
    lastPost: Option[Post],
    pageOf: Post ⇒ Int) {

  def id = topic.id
  def slug = topic.slug
  def name = topic.name
  def views = topic.views
  def createdAt = topic.createdAt
  def nbPosts = topic.nbPosts
}
