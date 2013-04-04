package lila.message

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

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  lazy val tube = Tube(
    reader = (__.json update (
      readDate('createdAt)
    )) andThen Json.reads[Post],
    writer = Json.writes[Post],
    writeTransformer = (__.json update (
      writeDate('createdAt)
    )).some
  )
}
