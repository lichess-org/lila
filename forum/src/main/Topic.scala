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

object Topics {

  val idSize = 8

  def apply(
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
}
