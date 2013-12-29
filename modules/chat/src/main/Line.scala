package lila.chat

import org.apache.commons.lang3.StringEscapeUtils.escapeXml
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json.Json
import play.api.templates.Html

import lila.user.User

case class Line(
    id: String,
    chan: Chan,
    username: String,
    to: Option[String],
    date: DateTime,
    text: String,
    troll: Boolean) {

  def system = username == Chat.systemUsername

  def html = Html {
    escapeXml(text)
  }

  def toJson = Json.obj(
    "chan" -> chan.key,
    "user" -> username,
    "html" -> html.toString)

  def userId = username.toLowerCase
}

object Line {

  val idSize = 10

  def make(chan: Chan, user: User, text: String, to: Option[String] = None): Line = Line(
    id = Random nextString idSize,
    chan = chan,
    username = user.username,
    to = to,
    date = DateTime.now,
    text = text,
    troll = user.troll)

  def system(chan: Chan, text: String, to: Option[String] = None): Line = Line(
    id = Random nextString idSize,
    chan = chan,
    username = Chat.systemUsername,
    to = to,
    date = DateTime.now,
    text = text,
    troll = false)

  type ID = String

  object BSONFields {
    val id = "_id"
    val chan = "c"
    val username = "u"
    val to = "to"
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
      username = r str username,
      to = Some(r str to) filter (_.nonEmpty),
      text = r str text,
      date = r date date,
      troll = r bool troll)

    def writes(w: BSON.Writer, o: Line) = BSONDocument(
      id -> o.id,
      chan -> o.chan.key,
      username -> o.username,
      to -> ~o.to,
      text -> o.text,
      date -> o.date,
      troll -> o.troll)
  }

  private[chat] lazy val tube = lila.db.BsTube(lineBSONHandler)
}
