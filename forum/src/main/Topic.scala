package lila.forum

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Topic(
    id: String,
    categId: String,
    slug: String,
    name: String,
    views: Int,
    createdAt: DateTime,
    updatedAt: DateTime,
    nbPosts: Int = 0,
    lastPostId: String = "") {

  def incNbPosts = copy(nbPosts = nbPosts + 1)
}

object Topic {

  val idSize = 8

  def make(
    categId: String,
    slug: String,
    name: String): Topic = Topic(
    id = Random nextString idSize,
    categId = categId,
    slug = slug,
    name = name,
    views = 0,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def postTube = Post.tube

  private def defaults = Json.obj(
    "nbPosts" -> 0,
    "lastPostId" -> "")

  private[forum] lazy val tube = Tube(
    reader = (__.json update (
      merge(defaults) andThen readDate('createdAt) andThen readDate('updatedAt)
    )) andThen Json.reads[Topic],
    writer = Json.writes[Topic],
    writeTransformer = (__.json update (
      writeDate('createdAt) andThen readDate('updatedAt)
    )).some
  )
}
