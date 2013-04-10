package lila.forum

import lila.db.Implicits._
import lila.db.api._
import tube.topicTube

import play.api.libs.json.Json

object TopicRepo {

  def byCateg(categ: Categ): Fu[List[Topic]] = 
    $find(byCategQuery(categ))

  def byTree(categSlug: String, slug: String): Fu[Option[Topic]] = 
    $find one Json.obj("categId" -> categSlug, "slug" -> slug)

  def nextSlug(categ: Categ, name: String, it: Int = 1): Fu[String] = {
    val slug = lila.common.String.slugify(name) + ~(it == 1).option("-" + it)
    byTree(categ.slug, slug) flatMap {
      _.isDefined.fold(
        nextSlug(categ, name, it + 1),
        fuccess(slug)
      )
    }
  }

  def incViews(topic: Topic): Funit = 
    $update($select(topic.id), $inc("views" -> 1))

  def byCategQuery(categ: Categ) = Json.obj("categId" -> categ.slug)
}
