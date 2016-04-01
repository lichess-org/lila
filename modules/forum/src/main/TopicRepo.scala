package lila.forum

import lila.db.dsl._

object TopicRepo extends TopicRepo(false) {

  def apply(troll: Boolean): TopicRepo = troll.fold(TopicRepoTroll, TopicRepo)
}

object TopicRepoTroll extends TopicRepo(true)

sealed abstract class TopicRepo(troll: Boolean) {

  import BSONHandlers.TopicBSONHandler

  private lazy val trollFilter = troll.fold(
    Json.obj(),
    Json.obj("troll" -> false)
  )

  def close(id: String, value: Boolean): Funit =
    $update.field(id, "closed", value)

  def hide(id: String, value: Boolean): Funit =
    $update.field(id, "hidden", value)

  def byCateg(categ: Categ): Fu[List[Topic]] =
    $find(byCategQuery(categ))

  def byTree(categSlug: String, slug: String): Fu[Option[Topic]] =
    $find.one(Json.obj("categId" -> categSlug, "slug" -> slug) ++ trollFilter)

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
    $update($select(topic.id), $inc("views" -> 1))

  def byCategQuery(categ: Categ) = Json.obj("categId" -> categ.slug) ++ trollFilter
}
