package lila.message

import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Post(
    id: String,
    text: String,
    isByCreator: Boolean,
    isRead: Boolean,
    createdAt: DateTime
) {

  def isByInvited = !isByCreator

  def isUnRead = !isRead

  def similar(other: Post) = text == other.text && isByCreator == other.isByCreator

  def erase = copy(text = "<deleted>")
}

object Post {

  val idSize = 8

  def make(
      text: String,
      isByCreator: Boolean
  ): Post = Post(
    id = Random nextString idSize,
    text = text,
    isByCreator = isByCreator,
    isRead = false,
    createdAt = DateTime.now
  )

  import lila.db.dsl.BSONJodaDateTimeHandler
  implicit private[message] val PostBSONHandler = reactivemongo.api.bson.Macros.handler[Post]
}
