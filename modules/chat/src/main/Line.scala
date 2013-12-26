package lila.chat

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

case class Line(
    id: String,
    chan: Chan,
    userId: String,
    date: DateTime,
    text: String,
    troll: Boolean) {

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

  type ID = String

  object BSONFields {
    val id = "_id"
    val chanType = "ct"
    val chanId = "ci"
    val userId = "u"
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
      chan = Chan(r str chanType, r str chanId),
      userId = r str userId,
      text = r str text,
      date = r date date,
      troll = r bool troll)

    def writes(w: BSON.Writer, o: Line) = BSONDocument(
      id -> o.id,
      chanType -> o.chan.typ,
      chanId -> o.chan.id,
      userId -> o.userId,
      text -> o.text,
      date -> o.date,
      troll -> o.troll)
  }

  private[chat] lazy val tube = lila.db.BsTube(lineBSONHandler)
}
