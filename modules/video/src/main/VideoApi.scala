package lila.video

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.core.commands._
import scala.concurrent.duration._

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator.Adapter
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
      videoColl.find($doc("_id" -> id)).uno[Video]

    def search(user: Option[User], query: String, page: Int): Fu[Paginator[VideoView]] = {
      val q = query.split(' ').map { word => s""""$word"""" } mkString " "
      val textScore = $doc("score" -> $doc("$meta" -> "textScore"))
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc(
            "$text" -> $doc("$search" -> q)
          ),
          projection = textScore,
          sort = textScore
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage)
    }

    def save(video: Video): Funit =
      videoColl.update(
        $doc("_id" -> video.id),
        $doc("$set" -> video),
        upsert = true).void

    def removeNotIn(ids: List[Video.ID]) =
      videoColl.remove(
        $doc("_id" $nin (ids: _*))
      ).void

    def setMetadata(id: Video.ID, metadata: Youtube.Metadata) =
      videoColl.update(
        $doc("_id" -> id),
        $doc("$set" -> $doc("metadata" -> metadata)),
        upsert = false
      ).void

    def allIds: Fu[List[Video.ID]] =
      videoColl.distinct("_id", none) map lila.db.BSON.asStrings

    def popular(user: Option[User], page: Int): Fu[Paginator[VideoView]] = Paginator(
      adapter = new Adapter[Video](
        collection = videoColl,
        selector = $empty,
        projection = $empty,
        sort = $doc("metadata.likes" -> -1)
      ) mapFutureList videoViews(user),
      currentPage = page,
      maxPerPage = maxPerPage)

    def byTags(user: Option[User], tags: List[Tag], page: Int): Fu[Paginator[VideoView]] =
      if (tags.isEmpty) popular(user, page)
      else Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc("tags" $all tags),
          projection = $empty,
          sort = $doc("metadata.likes" -> -1)
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage)

    def byAuthor(user: Option[User], author: String, page: Int): Fu[Paginator[VideoView]] =
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc("author" -> author),
          projection = $empty,
          sort = $doc("metadata.likes" -> -1)
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage)

    def similar(user: Option[User], video: Video, max: Int): Fu[Seq[VideoView]] =
      videoColl.find($doc(
        "tags" $in (video.tags: _*),
        "_id" $ne video.id
      )).sort($doc("metadata.likes" -> -1))
        .cursor[Video]()
        .gather[List]().map { videos =>
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
      viewColl.find($doc(
        View.BSONFields.id -> View.makeId(videoId, userId)
      )).uno[View]

    def add(a: View) = (viewColl insert a).void recover
      lila.db.recoverDuplicateKey(_ => ())

    def hasSeen(user: User, video: Video): Fu[Boolean] =
      viewColl.count($doc(
        View.BSONFields.id -> View.makeId(video.id, user.id)
      ).some) map (0!=)

    def seenVideoIds(user: User, videos: Seq[Video]): Fu[Set[Video.ID]] =
      viewColl.distinct(View.BSONFields.videoId,
        $inIds(videos.map { v =>
          View.makeId(v.id, user.id)
        }).some) map lila.db.BSON.asStringSet
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
            Match($doc("tags" $all filterTags)),
            List(Project($doc("tags" -> true)),
              Unwind("tags"), GroupField("tags")("nb" -> SumValue(1)))).map(
              _.documents.flatMap(_.asOpt[TagNb]))

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
        Project($doc("tags" -> true)), List(
          Unwind("tags"), GroupField("tags")("nb" -> SumValue(1)),
          Sort(Descending("nb")))).map(
          _.documents.flatMap(_.asOpt[TagNb])),
      timeToLive = 1.day)
  }
}
