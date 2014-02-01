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
    id = Random nextStringUppercase idSize,
    text = text,
    isByCreator = isByCreator,
    isRead = false,
    createdAt = DateTime.now)

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private[message] lazy val tube = JsTube(
    (__.json update readDate('createdAt)) andThen Json.reads[Post],
    Json.writes[Post].andThen(__.json update writeDate('createdAt))
  )
}
