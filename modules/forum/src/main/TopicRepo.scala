package lila.forum

import lila.db.dsl._

import reactivemongo.api.Cursor

object TopicRepo extends TopicRepo(false) {

  def apply(troll: Boolean): TopicRepo = if (troll) TopicRepoTroll else TopicRepo
}

object TopicRepoTroll extends TopicRepo(true)

sealed abstract class TopicRepo(troll: Boolean) {
  import reactivemongo.api.WriteConcern
  import BSONHandlers.TopicBSONHandler

  // dirty
  private val coll = Env.current.topicColl

  private val trollFilter = !troll ?? $doc("troll" -> false)
  private val notStickyQuery = $doc("sticky" $ne true)
  private val stickyQuery = $doc("sticky" -> true)

  def close(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "closed", value).void

  def hide(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "hidden", value).void

  def sticky(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "sticky", value).void

  def byCateg(categ: Categ): Fu[List[Topic]] =
    coll.find(byCategQuery(categ)).cursor[Topic]().list

  def countByCateg(categ: Categ): Fu[Int] =
    coll.countSel(byCategQuery(categ))

  def byTree(categSlug: String, slug: String): Fu[Option[Topic]] =
    coll.find(
      $doc("categId" -> categSlug, "slug" -> slug) ++ trollFilter
    ).one[Topic]

  def existsByTree(categSlug: String, slug: String): Fu[Boolean] =
    coll.exists($doc("categId" -> categSlug, "slug" -> slug))

  def stickyByCateg(categ: Categ): Fu[List[Topic]] =
    coll.find(byCategQuery(categ) ++ stickyQuery).cursor[Topic]().list

  def nextSlug(categ: Categ, name: String, it: Int = 1): Fu[String] = {
    val slug = Topic.nameToId(name) + ~(it != 1).option("-" + it)

    // also take troll topic into accounts
    TopicRepoTroll.byTree(categ.slug, slug) flatMap { found =>
      if (found.isDefined) nextSlug(categ, name, it + 1)
      else fuccess(slug)
    }
  }

  def incViews(topic: Topic): Unit = {
    coll.update(false, WriteConcern.Unacknowledged).
      one(q = $id(topic.id), u = $inc("views" -> 1))

    ()
  }

  def byCategQuery(categ: Categ) = $doc("categId" -> categ.slug) ++ trollFilter
  def byCategNotStickyQuery(categ: Categ) = byCategQuery(categ) ++ notStickyQuery
}
