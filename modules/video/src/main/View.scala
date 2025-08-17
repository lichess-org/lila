package lila.video

case class View(
    id: String, // userId/videoId
    videoId: Video.ID,
    userId: UserId,
    date: Instant
)

case class VideoView(video: Video, view: Boolean)

object View:

  def makeId(videoId: Video.ID, userId: UserId) = s"$videoId/$userId"

  def make(videoId: Video.ID, userId: UserId) =
    View(
      id = makeId(videoId, userId),
      videoId = videoId,
      userId = userId,
      date = nowInstant
    )

  object BSONFields:
    val id = "_id"
    val videoId = "v"
    val userId = "u"
    val date = "d"

  import reactivemongo.api.bson.*
  import lila.db.dsl.given
  import lila.db.BSON

  given BSONDocumentHandler[View] = new BSON[View]:

    import BSONFields.*

    def reads(r: BSON.Reader): View =
      View(
        id = r.str(id),
        videoId = r.str(videoId),
        userId = r.get[UserId](userId),
        date = r.get[Instant](date)
      )

    def writes(w: BSON.Writer, o: View) =
      BSONDocument(
        id -> o.id,
        videoId -> o.videoId,
        userId -> o.userId,
        date -> o.date
      )
