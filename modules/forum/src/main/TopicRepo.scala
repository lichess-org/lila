package lila.forum

import lila.db.dsl._

object TopicRepo extends TopicRepo(false) {

  def apply(troll: Boolean): TopicRepo = troll.fold(TopicRepoTroll, TopicRepo)
}

object TopicRepoTroll extends TopicRepo(true)

sealed abstract class TopicRepo(troll: Boolean) {

  import BSONHandlers.TopicBSONHandler

  // dirty
  private val coll = Env.current.topicColl

  private val trollFilter = troll.fold($empty, $doc("troll" -> false))

  def close(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "closed", value).void

  def hide(id: String, value: Boolean): Funit =
    coll.updateField($id(id), "hidden", value).void

  def byCateg(categ: Categ): Fu[List[Topic]] =
    coll.list[Topic](byCategQuery(categ))

  def byTree(categSlug: String, slug: String): Fu[Option[Topic]] =
    coll.one[Topic]($doc("categId" -> categSlug, "slug" -> slug) ++ trollFilter)

  def nextSlug(categ: Categ, name: String, it: Int = 1): Fu[String] = {
    val slug = Topic.nameToId(name) + ~(it != 1).option("-" + it)
    // also take troll topic into accounts
    TopicRepoTroll.byTree(categ.slug, slug) flatMap {
      _.isDefined.fold(
        nextSlug(categ, name, it + 1),
        fuccess(slug)
      )
    }
  }

  def incViews(topic: Topic): Funit =
    coll.update($id(topic.id), $inc("views" -> 1)).void

  def byCategQuery(categ: Categ) = $doc("categId" -> categ.slug) ++ trollFilter
}
