package lila.video

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._
import scala.concurrent.duration._
import spray.caching.{ LruCache, Cache }

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[video] final class VideoApi(
    videoColl: Coll,
    viewColl: Coll,
    filterColl: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val YoutubeBSONHandler = {
    import Youtube.Metadata
    Macros.handler[Metadata]
  }
  private implicit val VideoBSONHandler = Macros.handler[Video]
  private implicit val TagNbBSONHandler = Macros.handler[TagNb]
  private implicit val FilterBSONHandler = Macros.handler[Filter]
  import View.viewBSONHandler

  object video {

    def find(id: Video.ID): Fu[Option[Video]] =
      videoColl.find(BSONDocument("_id" -> id)).one[Video]

    def save(video: Video): Funit =
      videoColl.update(
        BSONDocument("_id" -> video.id),
        BSONDocument("$set" -> video),
        upsert = true).void

    def removeNotIn(ids: List[Video.ID]) =
      videoColl.remove(
        BSONDocument("_id" -> BSONDocument("$nin" -> ids))
      ).void

    def setMetadata(id: Video.ID, metadata: Youtube.Metadata) =
      videoColl.update(
        BSONDocument("_id" -> id),
        BSONDocument("$set" -> BSONDocument("metadata" -> metadata)),
        upsert = false
      ).void

    def allIds: Fu[List[Video.ID]] =
      videoColl.find(
        BSONDocument(),
        BSONDocument("_id" -> true)
      ).cursor[BSONDocument].collect[List]() map { doc =>
          doc flatMap (_.getAs[String]("_id"))
        }

    def popular(max: Int): Fu[List[Video]] =
      videoColl.find(BSONDocument())
        .sort(BSONDocument("metadata.likes" -> -1))
        .cursor[Video]
        .collect[List](max)

    def byTags(tags: List[Tag], max: Int): Fu[List[Video]] =
      if (tags.isEmpty) popular(max)
      else videoColl.find(BSONDocument(
        "tags" -> BSONDocument("$all" -> tags)
      )).sort(BSONDocument(
        "metadata.likes" -> -1
      )).cursor[Video]
        .collect[List]()

    def byAuthor(author: String): Fu[List[Video]] =
      videoColl.find(BSONDocument(
        "author" -> author
      )).sort(BSONDocument(
        "metadata.likes" -> -1
      )).cursor[Video]
        .collect[List]()

    def similar(video: Video, max: Int): Fu[List[Video]] =
      videoColl.find(BSONDocument(
        "tags" -> BSONDocument("$in" -> video.tags),
        "_id" -> BSONDocument("$ne" -> video.id)
      )).sort(BSONDocument("metadata.likes" -> -1))
        .cursor[Video]
        .collect[List]().map { videos =>
          videos.sortBy { v => -v.similarity(video) } take max
        }
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

  object tag {

    private val cache: Cache[List[TagNb]] = LruCache(timeToLive = 1.day)

    def clearCache = fuccess(cache.clear)

    def popular(max: Int): Fu[List[TagNb]] = cache(max) {
      import reactivemongo.core.commands._
      val command = Aggregate(videoColl.name, Seq(
        Project("tags" -> BSONBoolean(true)),
        Unwind("tags"),
        GroupField("tags")("nb" -> SumValue(1)),
        Sort(Seq(Descending("nb"))),
        Limit(max)
      ))
      videoColl.db.command(command) map {
        _.toList.flatMap(_.asOpt[TagNb])
      }
    }
  }

  object filter {

    def get(userId: String) =
      filterColl.find(
        BSONDocument("_id" -> userId)
      ).one[Filter] map (_ | Filter(userId, Nil, DateTime.now))

    def set(filter: Filter) =
      filterColl.update(
        BSONDocument("_id" -> filter.id),
        BSONDocument("$set" -> filter.copy(
          date = DateTime.now
        )),
        upsert = true).void
  }

  def userControl(userId: String, maxTags: Int): Fu[UserControl] =
    filter.get(userId) zip tag.popular(maxTags) map (UserControl.apply _).tupled
}
