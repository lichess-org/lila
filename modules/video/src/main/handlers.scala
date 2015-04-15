package lila.video

import lila.db.BSON
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson.Macros

private[video] object handlers {

  implicit val YoutubeBSONHandler = {
    import Youtube.Metadata
    Macros.handler[Metadata]
  }

  implicit val VideoBSONHandler = Macros.handler[Video]

  implicit val TagNbBSONHandler = Macros.handler[TagNb]

  implicit val viewBSONHandler = new BSON[View] {

    import View.BSONFields._

    def reads(r: BSON.Reader): View = View(
      id = r str id,
      videoId = r str videoId,
      userId = r str userId,
      date = r.get[DateTime](date))

    def writes(w: BSON.Writer, o: View) = BSONDocument(
      id -> o.id,
      videoId -> o.videoId,
      userId -> o.userId,
      date -> o.date)
  }
}
