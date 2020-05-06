package lila.video

import org.joda.time.DateTime

case class View(
    id: String, // userId/videoId
    videoId: Video.ID,
    userId: String,
    date: DateTime
)

case class VideoView(video: Video, view: Boolean)

object View {

  def makeId(videoId: Video.ID, userId: String) = s"$videoId/$userId"

  def make(videoId: Video.ID, userId: String) =
    View(
      id = makeId(videoId, userId),
      videoId = videoId,
      userId = userId,
      date = DateTime.now
    )

  object BSONFields {
    val id      = "_id"
    val videoId = "v"
    val userId  = "u"
    val date    = "d"
  }

  import reactivemongo.api.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit val viewBSONHandler = new BSON[View] {

    import BSONFields._

    def reads(r: BSON.Reader): View =
      View(
        id = r str id,
        videoId = r str videoId,
        userId = r str userId,
        date = r.get[DateTime](date)
      )

    def writes(w: BSON.Writer, o: View) =
      BSONDocument(
        id      -> o.id,
        videoId -> o.videoId,
        userId  -> o.userId,
        date    -> o.date
      )
  }
}
