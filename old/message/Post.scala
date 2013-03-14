package lila.app
package message

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Post(
    id: String,
    text: String,
    isByCreator: Boolean,
    isRead: Boolean,
    createdAt: DateTime) {

  def isByInvited = !isByCreator

  def isUnRead = !isRead
}

object Post {

  val idSize = 8

  def make(
    text: String,
    isByCreator: Boolean): Post = Post(
    id = Random nextString idSize,
    text = text,
    isByCreator = isByCreator,
    isRead = false,
    createdAt = DateTime.now)

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val json = mongodb.JsonTube((
    (__ \ 'id).read[String] and
    (__ \ 'text).read[String] and
    (__ \ 'isByCreator).read[Boolean] and
    (__ \ 'isRead).read[Boolean] and
    (__ \ 'createdAt).read[DateTime]
  )(Post.apply _),
    Json.writes[Post]
  )
}
