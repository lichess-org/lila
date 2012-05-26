package lila
package forum

case class CategView(
    categ: Categ,
    lastPost: Option[(Topic, Post)],
    pageOf: Post ⇒ Int) {

  def slug = categ.slug
  def name = categ.name
  def desc = categ.desc
  def nbTopics = categ.nbTopics
  def nbPosts = categ.nbPosts
}
