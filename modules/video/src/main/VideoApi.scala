package lila.video

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[video] final class VideoApi(
    videoColl: Coll,
    viewColl: Coll) {

  import Video.VideoBSONHandler

  object video {

    def find(id: Video.ID): Fu[Option[Video]] =
      videoColl.find(BSONDocument("_id" -> id)).one[Video]

    def save(video: Video): Fu[Video.ID] =
      videoColl.update(
        BSONDocument("_id" -> video.id),
        video,
        upsert = true)
  }

  object view {

    def find(videoId: Video.ID, userId: String): Fu[Option[View]] =
      viewColl.find(BSONDocument(
        View.BSONFields.id -> View.makeId(videoId, userId)
      )).one[View]

    def add(a: View) = viewColl insert a void

    def hasSeen(user: User, video: Video): Fu[Boolean] =
      viewColl.db command Count(viewColl.name, BSONDocument(
        View.BSONFields.id -> View.makeId(video.id, user.id)
      ).some) map (0!=)
  }
}
