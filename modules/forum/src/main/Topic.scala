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
    nbPosts: Int,
    lastPostId: String,
    updatedAtTroll: DateTime,
    nbPostsTroll: Int,
    lastPostIdTroll: String,
    troll: Boolean,
    closed: Boolean) {

  def open = !closed

  def withPost(post: Post): Topic = copy(
    nbPosts = post.troll.fold(nbPosts, nbPosts + 1),
    lastPostId = post.troll.fold(lastPostId, post.id),
    updatedAt = post.troll.fold(updatedAt, post.createdAt),
    nbPostsTroll = nbPostsTroll + 1,
    lastPostIdTroll = post.id,
    updatedAtTroll = post.createdAt)

  def incNbPosts = copy(nbPosts = nbPosts + 1)
}

object Topic {

  val idSize = 8

  def make(
    categId: String,
    slug: String,
    name: String,
    troll: Boolean): Topic = Topic(
    id = Random nextString idSize,
    categId = categId,
    slug = slug,
    name = name,
    views = 0,
    createdAt = DateTime.now,
    updatedAt = DateTime.now,
    nbPosts = 0,
    lastPostId = "",
    updatedAtTroll = DateTime.now,
    nbPostsTroll = 0,
    lastPostIdTroll = "",
    troll = troll,
    closed = false)

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def postTube = Post.tube

  private def defaults = Json.obj("closed" -> false)

  private[forum] lazy val tube = Tube(
    (__.json update (
      merge(defaults) andThen
      readDate('createdAt) andThen
      readDate('updatedAt) andThen
      readDate('updatedAtTroll)
    )) andThen Json.reads[Topic],
    Json.writes[Topic] andThen (__.json update (
      writeDate('createdAt) andThen
      writeDate('updatedAt) andThen
      writeDate('updatedAtTroll)
    ))
  )
}
