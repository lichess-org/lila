package lila
package forum

case class CategView(
    categ: Categ) {

  def slug = categ.slug
  def name = categ.name
  def desc = categ.desc
}
