package lila.video

import reactivemongo.api.bson.*
import scalalib.paginator.*

import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.memo.CacheApi.*

final private[video] class VideoApi(
    videoColl: Coll,
    viewColl: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  private given BSONDocumentHandler[Youtube.Metadata] = Macros.handler
  private given BSONDocumentHandler[Video] = Macros.handler
  private given BSONDocumentHandler[TagNb] = Macros.handler
  import View.given

  private def videoViews(userOption: Option[UserId])(videos: Seq[Video]): Fu[Seq[VideoView]] =
    userOption match
      case None =>
        fuccess:
          videos.map { VideoView(_, view = false) }
      case Some(user) =>
        view
          .seenVideoIds(user, videos)
          .map: ids =>
            videos.map: v =>
              VideoView(v, ids contains v.id)

  object video:

    private val maxPerPage = MaxPerPage(18)

    def find(id: Video.ID): Fu[Option[Video]] =
      videoColl.find($id(id)).one[Video]

    def search(user: Option[UserId], query: String, page: Int): Fu[Paginator[VideoView]] =
      val q = query
        .split(' ')
        .map: word =>
          s""""$word""""
        .mkString(" ")
      val textScore = $doc("score" -> $doc("$meta" -> "textScore"))
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $text(q),
          projection = textScore.some,
          sort = textScore,
          _.sec
        ).mapFutureList(videoViews(user)),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def save(video: Video): Funit =
      videoColl.update
        .one(
          $id(video.id),
          $doc("$set" -> video),
          upsert = true
        )
        .void

    def removeNotIn(ids: List[Video.ID]): Fu[Int] =
      videoColl.delete.one($doc("_id".$nin(ids))).map(_.n)

    def setMetadata(id: Video.ID, metadata: Youtube.Metadata) =
      videoColl.update
        .one(
          $id(id),
          $doc("$set" -> $doc("metadata" -> metadata)),
          upsert = false
        )
        .void

    def allIds: Fu[List[Video.ID]] =
      videoColl.distinctEasy[String, List]("_id", $empty, _.sec)

    def popular(user: Option[UserId], page: Int): Fu[Paginator[VideoView]] =
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $empty,
          projection = none,
          sort = $doc("metadata.likes" -> -1),
          _.sec
        ).mapFutureList(videoViews(user)),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def byTags(user: Option[UserId], tags: List[Tag], page: Int): Fu[Paginator[VideoView]] =
      if tags.isEmpty then popular(user, page)
      else
        Paginator(
          adapter = new Adapter[Video](
            collection = videoColl,
            selector = $doc("tags".$all(tags)),
            projection = none,
            sort = $doc("metadata.likes" -> -1),
            _.sec
          ).mapFutureList(videoViews(user)),
          currentPage = page,
          maxPerPage = maxPerPage
        )

    def byAuthor(user: Option[UserId], author: String, page: Int): Fu[Paginator[VideoView]] =
      Paginator(
        adapter = new Adapter[Video](
          collection = videoColl,
          selector = $doc("author" -> author),
          projection = none,
          sort = $doc("metadata.likes" -> -1),
          _.sec
        ).mapFutureList(videoViews(user)),
        currentPage = page,
        maxPerPage = maxPerPage
      )

    def similar(user: Option[UserId], video: Video, max: Int): Fu[Seq[VideoView]] =
      videoColl
        .aggregateList(maxDocs = max, _.sec): framework =>
          import framework.*
          Match(
            $doc(
              "tags".$in(video.tags),
              "_id".$ne(video.id)
            )
          ) -> List(
            AddFields:
              $doc(
                "int" -> $doc(
                  "$size" -> $doc(
                    "$setIntersection" -> $arr("$tags", video.tags)
                  )
                )
              )
            ,
            Sort(
              Descending("int"),
              Descending("metadata.likes")
            ),
            Limit(max)
          )
        .map(_.flatMap(_.asOpt[Video]))
        .flatMap(videoViews(user))

    object count:

      private val cache = cacheApi.unit[Long]:
        _.refreshAfterWrite(3.hours).buildAsyncFuture(_ => videoColl.countAll)

      def apply: Fu[Long] = cache.getUnit

  object view:

    def find(videoId: Video.ID, userId: UserId): Fu[Option[View]] =
      viewColl
        .find(
          $doc(
            View.BSONFields.id -> View.makeId(videoId, userId)
          )
        )
        .one[View]

    def add(a: View) = viewColl.insert.one(a).void.recover(lila.db.recoverDuplicateKey(_ => ()))

    def hasSeen(user: UserId, video: Video): Fu[Boolean] =
      viewColl
        .countSel(
          $doc(
            View.BSONFields.id -> View.makeId(video.id, user)
          )
        )
        .map(0 !=)

    def seenVideoIds(user: UserId, videos: Seq[Video]): Fu[Set[Video.ID]] =
      viewColl.distinctEasy[String, Set](
        View.BSONFields.videoId,
        $inIds(videos.map: v =>
          View.makeId(v.id, user)),
        _.sec
      )

  object tag:

    def paths(filterTags: List[Tag]): Fu[List[TagNb]] = pathsCache.get(filterTags.sorted)

    def allPopular: Fu[List[TagNb]] = popularCache.getUnit

    private val max = 25

    private val pathsCache = cacheApi[List[Tag], List[TagNb]](32, "video.paths"):
      _.expireAfterAccess(10.minutes).buildAsyncFuture: filterTags =>
        val allPaths =
          if filterTags.isEmpty then
            allPopular.map: tags =>
              tags.filterNot(_.isNumeric)
          else
            videoColl
              .aggregateList(maxDocs = Int.MaxValue, _.sec): framework =>
                import framework.*
                Match($doc("tags".$all(filterTags))) -> List(
                  Project($doc("tags" -> true)),
                  UnwindField("tags"),
                  GroupField("tags")("nb" -> SumAll)
                )
              .dmap { _.flatMap(_.asOpt[TagNb]) }

        allPopular.zip(allPaths).map { case (all, paths) =>
          val tags = all
            .map: t =>
              paths.find(_._id == t._id).getOrElse(TagNb(t._id, 0))
            .filterNot(_.empty)
            .take(max)
          val missing = filterTags.filterNot: t =>
            tags.exists(_.tag == t)
          val list = tags.take(max - missing.size) ::: missing.flatMap: t =>
            all.find(_.tag == t)
          list.sortBy: t =>
            if filterTags contains t.tag then Int.MinValue
            else -t.nb
        }

    private val popularCache = cacheApi.unit[List[TagNb]]:
      _.refreshAfterWrite(1.day).buildAsyncFuture: _ =>
        videoColl
          .aggregateList(maxDocs = Int.MaxValue, _.sec): framework =>
            import framework.*
            Project($doc("tags" -> true)) -> List(
              UnwindField("tags"),
              GroupField("tags")("nb" -> SumAll),
              Sort(Descending("nb"))
            )
          .map:
            _.flatMap(_.asOpt[TagNb])
