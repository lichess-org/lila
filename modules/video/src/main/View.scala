package lila.video

import org.joda.time.DateTime

case class View(
  id: String, // userId/videoId
  videoId: Video.ID,
  userId: String,
  date: DateTime)

object View {

  def makeId(videoId: Opening.ID, userId: String) = s"$videoId/$userId"

  object BSONFields {
    val id = "_id"
    val videoId = "v"
    val userId = "u"
    val date = "d"
  }

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val viewBSONHandler = new BSON[View] {

    import BSONFields._

    def reads(r: BSON.Reader): View = View(
      id = r str id,
      videoId = r int videoId,
      userId = r str userId,
      date = r.get[DateTime](date))

    def writes(w: BSON.Writer, o: View) = BSONDocument(
      id -> o.id,
      videoId -> o.videoId,
      userId -> o.userId,
      date -> o.date)
  }
}
