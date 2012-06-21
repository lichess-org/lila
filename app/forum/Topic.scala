package lila
package forum

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import ornicar.scalalib.OrnicarRandom

case class Topic(
    @Key("_id") id: String,
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

  def apply(
    categId: String,
    slug: String,
    name: String): Topic = Topic(
    id = OrnicarRandom nextString idSize,
    categId = categId,
    slug = slug,
    name = name,
    views = 0,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)
}
