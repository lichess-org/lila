package lila.forum

import lila.db.dsl._

final class TopicRepo(val coll: Coll, troll: Boolean = false)(implicit ec: scala.concurrent.ExecutionContext) {

  def withTroll(t: Boolean) = if (t == troll) this else new TopicRepo(coll, t)

  import BSONHandlers.TopicBSONHandler

  private val trollFilter         = !troll ?? $doc("troll" -> false)
  private lazy val notStickyQuery = $doc("sticky" $ne true)
  private lazy val stickyQuery    = $doc("sticky" -> true)

  def close(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "closed", value).void

  def hide(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "hidden", value).void

  def sticky(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "sticky", value).void

  def byCateg(categ: Categ): Fu[List[Topic]] =
    coll.list[Topic](byCategQuery(categ))

  def countByCateg(categ: Categ): Fu[Int] =
    coll.countSel(byCategQuery(categ))

  def byTree(categSlug: String, slug: String): Fu[Option[Topic]] =
    coll.one[Topic]($doc("categId" -> categSlug, "slug" -> slug) ++ trollFilter)

  def existsByTree(categSlug: String, slug: String): Fu[Boolean] =
    coll.exists($doc("categId" -> categSlug, "slug" -> slug))

  def stickyByCateg(categ: Categ): Fu[List[Topic]] =
    coll.list[Topic](byCategQuery(categ) ++ stickyQuery)

  def nextSlug(categ: Categ, name: String, it: Int = 1): Fu[String] = {
    val slug = Topic.nameToId(name) + ~(it != 1).option("-" + it)
    // also take troll topic into accounts
    withTroll(true).byTree(categ.slug, slug) flatMap { found =>
      if (found.isDefined) nextSlug(categ, name, it + 1)
      else fuccess(slug)
    }
  }

  def incViews(topic: Topic) =
    coll.incFieldUnchecked($id(topic.id), "views")

  def byCategQuery(categ: Categ)          = $doc("categId" -> categ.slug) ++ trollFilter
  def byCategNotStickyQuery(categ: Categ) = byCategQuery(categ) ++ notStickyQuery
}
