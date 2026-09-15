package lila.ublog

import java.time.YearMonth

import reactivemongo.api.*
import reactivemongo.api.bson.BSONNull
import scalalib.paginator.{ AdapterLike, Paginator }

import scalalib.model.Language
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.core.ublog.{ BlogsBy, Quality, QualityFilter }

final class UblogPaginator(
    colls: UblogColls,
    ublogApi: UblogApi,
    relationApi: lila.core.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import UblogBsonHandlers.{ *, given }
  import UblogPost.PreviewPost
  import ublogApi.aggregateVisiblePosts

  val maxPerPage = MaxPerPage(24)

  def byUser[U: UserIdOf](user: U, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    byBlog(UblogBlog.Id.User(user.id), live, page)

  def byBlog(blog: UblogBlog.Id, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = Adapter[PreviewPost](
        collection = colls.post,
        selector = bdoc("blog" -> blog, "live" -> live) ++
          live.not.so(
            nor(bdoc("title" -> "", "intro" -> "", "markdown" -> "", "image" -> bdoc("$exists" -> false)))
          ),
        projection = previewPostProjection.some,
        sort = if live then userLiveSort else bdoc("created.at" -> -1),
        _.sec
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByCommunity(
      language: Option[Language],
      filter: QualityFilter,
      page: Int
  ): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost]:
        val select =
          bdoc("live" -> true, selectQuality(filter), "topics".neq(UblogTopic.offTopic)) ++
            language.so(l => bdoc("language" -> l))
        def nbResults: Fu[Int] = fuccess(50 * maxPerPage.value)
        def slice(offset: Int, length: Int) = aggregateVisiblePosts(select, offset, length)
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByLiked(page: Int)(using me: Me): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = Adapter[PreviewPost](
        collection = colls.post,
        selector = bdoc("live" -> true, "likers" -> me.userId),
        projection = previewPostProjection.some,
        sort = sort.desc("lived.at"),
        _.sec
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByTopic(
      topic: UblogTopic,
      filter: QualityFilter,
      by: BlogsBy,
      page: Int
  ): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost]:
        def nbResults: Fu[Int] = fuccess(50 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          aggregateVisiblePosts(
            bdoc("topics" -> topic, selectQuality(filter, topic == UblogTopic.offTopic)),
            offset,
            length,
            by
          )
      ,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByMonth(
      month: YearMonth,
      filter: QualityFilter,
      by: BlogsBy,
      page: Int
  ): Fu[Paginator[PreviewPost]] =
    UblogByMonth
      .isValid(month)
      .so:
        Paginator(
          adapter = new AdapterLike[PreviewPost]:
            def nbResults: Fu[Int] = fuccess(50 * maxPerPage.value)
            def slice(offset: Int, length: Int) =
              // topics included to hit prod index
              aggregateVisiblePosts(
                UblogByMonth.selector(month) ++ selectQuality(filter),
                offset,
                length,
                by
              )
          ,
          currentPage = page,
          maxPerPage = maxPerPage
        )

  private def selectQuality(filter: QualityFilter, offTopic: Boolean = false): Bdoc =
    filter match
      case QualityFilter.all => bdoc("automod.quality".gte(if offTopic then Quality.spam else Quality.weak))
      case QualityFilter.best => bdoc("automod.quality".gte(if offTopic then Quality.weak else Quality.good))
      case QualityFilter.weak => bdoc("automod.quality" -> Quality.weak)
      case QualityFilter.spam => bdoc("automod.quality" -> Quality.spam)
      case QualityFilter.pending => pendingReviewSelect

  object liveByFollowed:

    def apply(user: User, page: Int): Fu[Paginator[PreviewPost]] =
      Paginator(
        adapter = new AdapterLike[PreviewPost]:
          def nbResults: Fu[Int] = fuccess(10 * maxPerPage.value)
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
            Match(bdoc("u1" -> userId, "r" -> lila.core.relation.Relation.Follow)) -> List(
              Group(BSONNull)("ids" -> PushField("u2")),
              PipelineOperator:
                lookup.pipelineFull(
                  from = colls.post.name,
                  as = "post",
                  let = bdoc("users" -> "$ids"),
                  pipe = List(
                    bdoc(
                      "$match" -> expr(
                        and(
                          bdoc("$in" -> barr(s"$$created.by", "$$users")),
                          bdoc("$eq" -> barr("$live", true)),
                          bdoc("$gt" -> barr("$lived.at", nowInstant.minusMonths(3)))
                        )
                      )
                    ),
                    bdoc("$project" -> previewPostProjection),
                    bdoc("$sort" -> bdoc("lived.at" -> -1)),
                    bdoc("$skip" -> offset),
                    bdoc("$limit" -> length)
                  )
                )
              ,
              Project(bdoc("ids" -> false, "_id" -> false)),
              UnwindField("post"),
              Limit(length),
              ReplaceRootField("post")
            )
          }
          .map: docs =>
            for
              doc <- docs
              post <- doc.asOpt[PreviewPost]
            yield post
