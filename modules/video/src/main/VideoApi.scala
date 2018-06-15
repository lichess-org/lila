package lila.video

import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

private[video] final class VideoApi(
    videoColl: Coll,
    viewColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder
) {

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

    private val maxPerPage = lila.common.MaxPerPage(18)

    def find(id: Video.ID): Fu[Option[Video]] =
      videoColl.find($id(id)).uno[Video]

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
          sort = textScore,
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage
      )
    }

    def save(video: Video): Funit =
      videoColl.update(
        $id(video.id),
        $doc("$set" -> video),
        upsert = true
      ).void

    def removeNotIn(ids: List[Video.ID]) =
      videoColl.remove($doc("_id" $nin ids)).void

    def setMetadata(id: Video.ID, metadata: Youtube.Metadata) =
      videoColl.update(
        $id(id),
        $doc("$set" -> $doc("metadata" -> metadata)),
        upsert = false
      ).void

    def allIds: Fu[List[Video.ID]] =
      videoColl.distinct[String, List]("_id", none)

    def popular(user: Option[User], page: Int): Fu[Paginator[VideoView]] = Paginator(
      adapter = new Adapter[Video](
        collection = videoColl,
        selector = $empty,
        projection = $empty,
        sort = $doc("metadata.likes" -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ) mapFutureList videoViews(user),
      currentPage = page,
      maxPerPage = maxPerPage
    )

    def byTags(user: Option[User], tags: List[Tag], page: Int): Fu[Paginator[VideoView]] =
      if (tags.isEmpty) popular(user, page)
      else Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc("tags" $all tags),
          projection = $empty,
          sort = $doc("metadata.likes" -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def byAuthor(user: Option[User], author: String, page: Int): Fu[Paginator[VideoView]] =
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc("author" -> author),
          projection = $empty,
          sort = $doc("metadata.likes" -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def similar(user: Option[User], video: Video, max: Int): Fu[Seq[VideoView]] = {
      import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
      videoColl.aggregateList(
        Match($doc(
          "tags" $in video.tags,
          "_id" $ne video.id
        )), List(
          AddFields($doc(
            "int" -> $doc(
              "$size" -> $doc(
                "$setIntersection" -> $arr("$tags", video.tags)
              )
            )
          )),
          Sort(
            Descending("int"),
            Descending("metadata.likes")
          ),
          Limit(max)
        ),
        maxDocs = max,
        ReadPreference.secondaryPreferred
      ).map(_.flatMap(_.asOpt[Video])) flatMap videoViews(user)
    }

    object count {

      private val cache = asyncCache.single(
        name = "video.count",
        f = videoColl.count(none),
        expireAfter = _.ExpireAfterWrite(3 hours)
      )

      def apply: Fu[Int] = cache.get
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
      viewColl.distinct[String, Set](
        View.BSONFields.videoId,
        $inIds(videos.map { v =>
          View.makeId(v.id, user.id)
        }).some
      )
  }

  object tag {

    def paths(filterTags: List[Tag]): Fu[List[TagNb]] = pathsCache get filterTags.sorted

    def allPopular: Fu[List[TagNb]] = popularCache.get

    private val max = 25

    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Descending, GroupField, Match, Project, UnwindField, Sort, SumValue }

    private val pathsCache = asyncCache.clearable[List[Tag], List[TagNb]](
      name = "video.paths",
      f = filterTags => {
        val allPaths =
          if (filterTags.isEmpty) allPopular map { tags =>
            tags.filterNot(_.isNumeric)
          }
          else videoColl.aggregateList(
            Match($doc("tags" $all filterTags)),
            List(Project($doc("tags" -> true)), UnwindField("tags"),
              GroupField("tags")("nb" -> SumValue(1))),
            maxDocs = Int.MaxValue,
            ReadPreference.secondaryPreferred
          ).map { _.flatMap(_.asOpt[TagNb]) }

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
      expireAfter = _.ExpireAfterAccess(10 minutes)
    )

    private val popularCache = asyncCache.single[List[TagNb]](
      name = "video.popular",
      f = videoColl.aggregateList(
        Project($doc("tags" -> true)), List(
          UnwindField("tags"), GroupField("tags")("nb" -> SumValue(1)),
          Sort(Descending("nb"))
        ),
        maxDocs = Int.MaxValue,
        readPreference = ReadPreference.secondaryPreferred
      ).map {
          _.flatMap(_.asOpt[TagNb])
        },
      expireAfter = _.ExpireAfterWrite(1.day)
    )
  }
}
