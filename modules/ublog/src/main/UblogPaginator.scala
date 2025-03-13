package lila.ublog

import java.time.{ YearMonth, ZoneOffset }

import reactivemongo.api.*
import reactivemongo.api.bson.BSONNull
import scalalib.paginator.{ AdapterLike, Paginator }

import scalalib.model.Language
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter

final class UblogPaginator(
    colls: UblogColls,
    ublogApi: UblogApi,
    relationApi: lila.core.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import UblogBsonHandlers.{ *, given }
  import UblogPost.PreviewPost
  import ublogApi.aggregateVisiblePosts

  val maxPerPage = MaxPerPage(9)

  def byUser[U: UserIdOf](user: U, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    byBlog(UblogBlog.Id.User(user.id), live, page)

  def byBlog(blog: UblogBlog.Id, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = Adapter[PreviewPost](
        collection = colls.post,
        selector = $doc("blog" -> blog, "live" -> live),
        projection = previewPostProjection.some,
        sort = if live then userLiveSort else $doc("created.at" -> -1),
        _.sec
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByCommunity(language: Option[Language], page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost]:
        val select = $doc("live" -> true, "topics".$ne(UblogTopic.offTopic)) ++ language.so: l =>
          $doc("language" -> l)
        def nbResults: Fu[Int]              = fuccess(10 * maxPerPage.value)
        def slice(offset: Int, length: Int) = aggregateVisiblePosts(select, offset, length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByLiked(page: Int)(using me: Me): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = Adapter[PreviewPost](
        collection = colls.post,
        selector = $doc("live" -> true, "likers" -> me.userId),
        projection = previewPostProjection.some,
        sort = $sort.desc("lived.at"),
        _.sec
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByTopic(topic: UblogTopic, page: Int, byDate: Boolean): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost]:
        def nbResults: Fu[Int] = fuccess(10 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          aggregateVisiblePosts($doc("topics" -> topic), offset, length, byDate)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  // All blogs ranked by `rank` lived during a specific month
  // TODO FIXME use `timeless` ranking instead of `rank`
  def liveByMonth(month: YearMonth, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost]:
        def nbResults: Fu[Int] = fuccess(10 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          // topics included to hit prod index
          aggregateVisiblePosts(UblogBestOf.selector(month), offset, length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  object liveByFollowed:

    def apply(user: User, page: Int): Fu[Paginator[PreviewPost]] =
      Paginator(
        adapter = new AdapterLike[PreviewPost]:
          def nbResults: Fu[Int]              = fuccess(10 * maxPerPage.value)
          def slice(offset: Int, length: Int) = cache.get((user.id, offset, length))
        ,
        currentPage = page,
        maxPerPage = maxPerPage
      )

    private val cache = cacheApi[(UserId, Int, Int), List[PreviewPost]](256, "ublog.paginator.followed"):
      _.expireAfterWrite(15.seconds).buildAsyncFuture: (userId, offset, length) =>
        relationApi.coll
          .aggregateList(length, _.sec) { framework =>
            import framework.*
            Match($doc("u1" -> userId, "r" -> lila.core.relation.Relation.Follow)) -> List(
              Group(BSONNull)("ids" -> PushField("u2")),
              PipelineOperator:
                $lookup.pipelineFull(
                  from = colls.post.name,
                  as = "post",
                  let = $doc("users" -> "$ids"),
                  pipe = List(
                    $doc(
                      "$match" -> $expr(
                        $and(
                          $doc("$in" -> $arr(s"$$created.by", "$$users")),
                          $doc("$eq" -> $arr("$live", true)),
                          $doc("$gt" -> $arr("$lived.at", nowInstant.minusMonths(3)))
                        )
                      )
                    ),
                    $doc("$project" -> previewPostProjection),
                    $doc("$sort"    -> $doc("lived.at" -> -1)),
                    $doc("$skip"    -> offset),
                    $doc("$limit"   -> length)
                  )
                )
              ,
              Project($doc("ids" -> false, "_id" -> false)),
              UnwindField("post"),
              Limit(length),
              ReplaceRootField("post")
            )
          }
          .map: docs =>
            for
              doc  <- docs
              post <- doc.asOpt[PreviewPost]
            yield post
