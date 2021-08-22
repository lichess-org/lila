package lila.video

import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.memo.CacheApi._
import lila.user.User

final private[video] class VideoApi(
    videoColl: Coll,
    viewColl: Coll,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  import reactivemongo.api.bson.Macros
  implicit private val YoutubeBSONHandler = {
    import Youtube.Metadata
    Macros.handler[Metadata]
  }
  implicit private val VideoBSONHandler = Macros.handler[Video]
  implicit private val TagNbBSONHandler = Macros.handler[TagNb]
  import View.viewBSONHandler

  private def videoViews(userOption: Option[User])(videos: Seq[Video]): Fu[Seq[VideoView]] =
    userOption match {
      case None =>
        fuccess {
          videos map { VideoView(_, view = false) }
        }
      case Some(user) =>
        view.seenVideoIds(user, videos) map { ids =>
          videos.map { v =>
            VideoView(v, ids contains v.id)
          }
        }
    }

  object video {

    private val maxPerPage = lila.common.config.MaxPerPage(18)

    def find(id: Video.ID): Fu[Option[Video]] =
      videoColl.find($id(id)).one[Video]

    def search(user: Option[User], query: String, page: Int): Fu[Paginator[VideoView]] = {
      val q = query.split(' ').map { word =>
        s""""$word""""
      } mkString " "
      val textScore = $doc("score" -> $doc("$meta" -> "textScore"))
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc(
            "$text" -> $doc("$search" -> q)
          ),
          projection = textScore.some,
          sort = textScore,
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage
      )
    }

    def save(video: Video): Funit =
      videoColl.update
        .one(
          $id(video.id),
          $doc("$set" -> video),
          upsert = true
        )
        .void

    def removeNotIn(ids: List[Video.ID]): Fu[Int] =
      videoColl.delete.one($doc("_id" $nin ids)).map(_.n)

    def setMetadata(id: Video.ID, metadata: Youtube.Metadata) =
      videoColl.update
        .one(
          $id(id),
          $doc("$set" -> $doc("metadata" -> metadata)),
          upsert = false
        )
        .void

    def allIds: Fu[List[Video.ID]] =
      videoColl.distinctEasy[String, List]("_id", $empty, ReadPreference.secondaryPreferred)

    def popular(user: Option[User], page: Int): Fu[Paginator[VideoView]] =
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $empty,
          projection = none,
          sort = $doc("metadata.likes" -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def byTags(user: Option[User], tags: List[Tag], page: Int): Fu[Paginator[VideoView]] =
      if (tags.isEmpty) popular(user, page)
      else
        Paginator(
          adapter = new Adapter[Video](
            collection = videoColl,
            selector = $doc("tags" $all tags),
            projection = none,
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
          projection = none,
          sort = $doc("metadata.likes" -> -1),
          readPreference = ReadPreference.secondaryPreferred
        ) mapFutureList videoViews(user),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def similar(user: Option[User], video: Video, max: Int): Fu[Seq[VideoView]] =
      videoColl
        .aggregateList(
          maxDocs = max,
          ReadPreference.secondaryPreferred
        ) { framework =>
          import framework._
          Match(
            $doc(
              "tags" $in video.tags,
              "_id" $ne video.id
            )
          ) -> List(
            AddFields(
              $doc(
                "int" -> $doc(
                  "$size" -> $doc(
                    "$setIntersection" -> $arr("$tags", video.tags)
                  )
                )
              )
            ),
            Sort(
              Descending("int"),
              Descending("metadata.likes")
            ),
            Limit(max)
          )
        }
        .map(_.flatMap(_.asOpt[Video])) flatMap videoViews(user)

    object count {

      private val cache = cacheApi.unit[Long] {
        _.refreshAfterWrite(3 hours)
          .buildAsyncFuture(_ => videoColl.countAll)
      }

      def apply: Fu[Long] = cache.getUnit
    }
  }

  object view {

    def find(videoId: Video.ID, userId: String): Fu[Option[View]] =
      viewColl
        .find(
          $doc(
            View.BSONFields.id -> View.makeId(videoId, userId)
          )
        )
        .one[View]

    def add(a: View) =
      (viewColl.insert.one(a)).void recover
        lila.db.recoverDuplicateKey(_ => ())

    def hasSeen(user: User, video: Video): Fu[Boolean] =
      viewColl.countSel(
        $doc(
          View.BSONFields.id -> View.makeId(video.id, user.id)
        )
      ) map (0 !=)

    def seenVideoIds(user: User, videos: Seq[Video]): Fu[Set[Video.ID]] =
      viewColl.distinctEasy[String, Set](
        View.BSONFields.videoId,
        $inIds(videos.map { v =>
          View.makeId(v.id, user.id)
        }),
        ReadPreference.secondaryPreferred
      )
  }

  object tag {

    def paths(filterTags: List[Tag]): Fu[List[TagNb]] = pathsCache get filterTags.sorted

    def allPopular: Fu[List[TagNb]] = popularCache.getUnit

    private val max = 25

    private val pathsCache = cacheApi[List[Tag], List[TagNb]](32, "video.paths") {
      _.expireAfterAccess(10 minutes)
        .buildAsyncFuture { filterTags =>
          val allPaths =
            if (filterTags.isEmpty) allPopular map { tags =>
              tags.filterNot(_.isNumeric)
            }
            else
              videoColl
                .aggregateList(
                  maxDocs = Int.MaxValue,
                  ReadPreference.secondaryPreferred
                ) { framework =>
                  import framework._
                  Match($doc("tags" $all filterTags)) -> List(
                    Project($doc("tags" -> true)),
                    UnwindField("tags"),
                    GroupField("tags")("nb" -> SumAll)
                  )
                }
                .dmap { _.flatMap(_.asOpt[TagNb]) }

          allPopular zip allPaths map { case (all, paths) =>
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
        }
    }

    private val popularCache = cacheApi.unit[List[TagNb]] {
      _.refreshAfterWrite(1.day)
        .buildAsyncFuture { _ =>
          videoColl
            .aggregateList(
              maxDocs = Int.MaxValue,
              readPreference = ReadPreference.secondaryPreferred
            ) { framework =>
              import framework._
              Project($doc("tags" -> true)) -> List(
                UnwindField("tags"),
                GroupField("tags")("nb" -> SumAll),
                Sort(Descending("nb"))
              )
            }
            .dmap {
              _.flatMap(_.asOpt[TagNb])
            }
        }
    }
  }
}
