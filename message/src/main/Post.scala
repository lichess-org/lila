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

object Posts {

  val idSize = 8

  def make(
    text: String,
    isByCreator: Boolean): Post = Post(
    id = Random nextString idSize,
    text = text,
    isByCreator = isByCreator,
    isRead = false,
    createdAt = DateTime.now)

  import lila.db.JsonTube
  import play.api.libs.json._
  import Reads.constraints._

  val json = JsonTube(
    reads = (__.json update (
      readDate('createdAt) 
    )) andThen Json.reads[Post],
    writes = Json.writes[Post],
    writeTransformer = (__.json update (
      writeDate('createdAt) 
    )).some
  )

  private def readDate(field: Symbol) = (__ \ field).json.update(of[JsObject] map (_ \ "$date"))
  private def writeDate(field: Symbol) = (__ \ field).json.update(of[JsNumber] map {
    millis ⇒ Json.obj("$date" -> millis)
  })
}
