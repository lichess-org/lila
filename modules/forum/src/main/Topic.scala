package lila.forum

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Topic(
    _id: String,
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
    closed: Boolean,
    hidden: Boolean) {

  def id = _id

  def updatedAt(troll: Boolean): DateTime = troll.fold(updatedAtTroll, updatedAt)
  def nbPosts(troll: Boolean): Int = troll.fold(nbPostsTroll, nbPosts)
  def nbReplies(troll: Boolean): Int = nbPosts(troll) - 1
  def lastPostId(troll: Boolean): String = troll.fold(lastPostIdTroll, lastPostId)

  def open = !closed
  def visibleOnHome = !hidden

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

  def nameToId(name: String) = (lila.common.String slugify name) |> { slug =>
    // if most chars are not latin, go for random slug
    (slug.size > (name.size / 2)).fold(slug, Random nextStringUppercase 8)
  }

  val idSize = 8

  def make(
    categId: String,
    slug: String,
    name: String,
    troll: Boolean,
    featured: Boolean): Topic = Topic(
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
    closed = false,
    hidden = !featured)
}
