package lila.video

import org.joda.time.DateTime

case class View(
  id: String, // userId/videoId
  videoId: Video.ID,
  userId: String,
  date: DateTime)

case class VideoView(video: Video, view: Boolean)

object View {

  def makeId(videoId: Video.ID, userId: String) = s"$videoId/$userId"

  def make(videoId: Video.ID, userId: String) = View(
    id = makeId(videoId, userId),
    videoId = videoId,
    userId = userId,
    date = DateTime.now)

  object BSONFields {
    val id = "_id"
    val videoId = "v"
    val userId = "u"
    val date = "d"
  }
}
