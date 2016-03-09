package lila.video

import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.common.paginator._
import lila.db.paginator.BSONAdapter
import lila.db.Types.Coll
import lila.memo.AsyncCache
import lila.user.{ User, UserRepo }

private[video] final class VideoApi(
    videoColl: Coll,
    viewColl: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.bson.Macros
  private implicit val YoutubeBSONHandler = {
    import Youtube.Metadata
    Macros.handler[Metadata]
  }
  private implicit val VideoBSONHandler = Macros.handler[Video]
  private implicit val TagNbBSONHandler = Macros.handler[TagNb]
  import View.viewBSONHandler

  private def videoViews(userOption: Option[User])(videos: Seq[Video]): Fu[Seq[VideoView]] = userOption match {
    case None => fuccess {
      videos map { VideoView(_, false) }
    }
    case Some(user) => view.seenVideoIds(user, videos) map { ids =>
      videos.map { v =>
        VideoView(v, ids contains v.id)
      }
    }
  }

  object video {

    private val maxPerPage = 18

    def find(id: Video.ID): Fu[Option[Video]] =
      videoColl.find(BSONDocument("_id" -> id)).one[Video]

    def search(user: Option[User], query: String, page: Int): Fu[Paginator[VideoView]] = {
      val q = query.split(' ').map { word => s""""$word"""" } mkString " "
      val textScore = BSONDocument("score" -> BSONDocument("$meta" -> "textScore"))
      Paginator(
        adapter = new BSONAdapter[Video](
          collection = videoColl,
          selector = BSONDocument(
            "$text" -> BSONDocument("$search" -> q)
          ),
          projection = textScore,
          sort = textScore,
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage)
    }

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

    def allIds: Fu[List[Video.ID]] = videoColl.distinct[String, List]("_id", none)

    def popular(user: Option[User], page: Int): Fu[Paginator[VideoView]] = Paginator(
      adapter = new BSONAdapter[Video](
        collection = videoColl,
        selector = BSONDocument(),
        projection = BSONDocument(),
        sort = BSONDocument("metadata.likes" -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ) mapFutureList videoViews(user),
      currentPage = page,
      maxPerPage = maxPerPage)

    def byTags(user: Option[User], tags: List[Tag], page: Int): Fu[Paginator[VideoView]] =
      if (tags.isEmpty) popular(user, page)
      else Paginator(
        adapter = new BSONAdapter[Video](
          collection = videoColl,
          selector = BSONDocument(
            "tags" -> BSONDocument("$all" -> tags)
          ),
          projection = BSONDocument(),
          sort = BSONDocument("metadata.likes" -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage)

    def byAuthor(user: Option[User], author: String, page: Int): Fu[Paginator[VideoView]] =
      Paginator(
        adapter = new BSONAdapter[Video](
          collection = videoColl,
          selector = BSONDocument(
            "author" -> author
          ),
          projection = BSONDocument(),
          sort = BSONDocument("metadata.likes" -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage)

    def similar(user: Option[User], video: Video, max: Int): Fu[Seq[VideoView]] =
      videoColl.find(BSONDocument(
        "tags" -> BSONDocument("$in" -> video.tags),
        "_id" -> BSONDocument("$ne" -> video.id)
      )).sort(BSONDocument("metadata.likes" -> -1))
        .cursor[Video](ReadPreference.secondaryPreferred)
        .collect[List]().map { videos =>
          videos.sortBy { v => -v.similarity(video) } take max
        } flatMap videoViews(user)

    object count {

      private val cache = AsyncCache.single(
        f = videoColl.count(none),
        timeToLive = 1.day)

      def clearCache = cache.clear

      def apply: Fu[Int] = cache apply true
    }
  }

  object view {

    def find(videoId: Video.ID, userId: String): Fu[Option[View]] =
      viewColl.find(BSONDocument(
        View.BSONFields.id -> View.makeId(videoId, userId)
      )).one[View]

    def add(a: View) = (viewColl insert a).void recover
      lila.db.recoverDuplicateKey(_ => ())

    def hasSeen(user: User, video: Video): Fu[Boolean] =
      viewColl.count(BSONDocument(
        View.BSONFields.id -> View.makeId(video.id, user.id)
      ).some) map (0!=)

    def seenVideoIds(user: User, videos: Seq[Video]): Fu[Set[Video.ID]] =
      viewColl.distinct[Video.ID, Set](View.BSONFields.videoId,
        BSONDocument(
          "_id" -> BSONDocument("$in" -> videos.map { v =>
            View.makeId(v.id, user.id)
          })
        ).some)
  }

  object tag {

    def paths(filterTags: List[Tag]): Fu[List[TagNb]] = pathsCache(filterTags.sorted)

    def allPopular: Fu[List[TagNb]] = popularCache(true)

    def clearCache = pathsCache.clear >> popularCache.clear

    private val max = 25

    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Descending, GroupField, Match, Project, Unwind, Sort, SumValue }

    private val pathsCache = AsyncCache[List[Tag], List[TagNb]](
      f = filterTags => {
        val allPaths =
          if (filterTags.isEmpty) allPopular map { tags =>
            tags.filterNot(_.isNumeric)
          }
          else videoColl.aggregate(
            Match(BSONDocument("tags" -> BSONDocument("$all" -> filterTags))),
            List(Project(BSONDocument("tags" -> BSONBoolean(true))),
              Unwind("tags"), GroupField("tags")("nb" -> SumValue(1)))).map(
              _.firstBatch.flatMap(_.asOpt[TagNb]))

        allPopular zip allPaths map {
          case (all, paths) =>
            val tags = all map { t =>
              paths find (_._id == t._id) getOrElse TagNb(t._id, 0)
            } filterNot (_.empty) take max
            val missing = filterTags filterNot { t =>
              tags exists (_.tag == t)
            }
            val list = tags.take(max - missing.size) ::: missing.flatMap { t =>
              all find (_.tag == t)
            }
            list.sortBy { t =>
              if (filterTags contains t.tag) Int.MinValue
              else -t.nb
            }
        }
      },
      maxCapacity = 100)

    private val popularCache = AsyncCache.single[List[TagNb]](
      f = videoColl.aggregate(
        Project(BSONDocument("tags" -> BSONBoolean(true))), List(
          Unwind("tags"), GroupField("tags")("nb" -> SumValue(1)),
          Sort(Descending("nb")))).map(
          _.firstBatch.flatMap(_.asOpt[TagNb])),
      timeToLive = 1.day)
  }
}
