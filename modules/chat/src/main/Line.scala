package lila.chat

import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json.Json
import play.api.templates.Html

import lila.user.User

case class Line(
    id: String,
    chan: Chan,
    userId: String,
    date: DateTime,
    text: String,
    troll: Boolean) {

  def system = userId == Chat.systemUserId
}

case class NamedLine(line: Line, username: String) {

  def toJson = Json.obj(
    "chan" -> Json.obj(
      "key" -> line.chan.key,
      "type" -> line.chan.typ
    ),
    "user" -> username,
    "html" -> line.text)
}

object Line {

  val idSize = 10

  def make(chan: Chan, user: User, text: String): Line = Line(
    id = Random nextString idSize,
    chan = chan,
    userId = user.id,
    date = DateTime.now,
    text = text,
    troll = user.troll)

  def system(chan: Chan, text: String): Line = Line(
    id = Random nextString idSize,
    chan = chan,
    userId = Chat.systemUserId,
    date = DateTime.now,
    text = text,
    troll = false)

  type ID = String

  object BSONFields {
    val id = "_id"
    val chan = "c"
    val userId = "ui"
    val date = "d"
    val text = "t"
    val troll = "tr"
  }

  import lila.db.BSON
  import lila.db.BSON.BSONJodaDateTimeHandler

  private def lineBSONHandler = new BSON[Line] {

    import BSONFields._
    import reactivemongo.bson.BSONDocument

    def reads(r: BSON.Reader): Line = Line(
      id = r str id,
      chan = Chan parse (r str chan) err s"Line has invalid chan: ${r.doc}",
      userId = r str userId,
      text = r str text,
      date = r date date,
      troll = r bool troll)

    def writes(w: BSON.Writer, o: Line) = BSONDocument(
      id -> o.id,
      chan -> o.chan.key,
      userId -> o.userId,
      text -> o.text,
      date -> o.date,
      troll -> o.troll)
  }

  private[chat] lazy val tube = lila.db.BsTube(lineBSONHandler)
}
