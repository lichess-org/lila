package lila.ublog

import reactivemongo.api.*

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.user.User
import reactivemongo.api.bson.BSONNull
import play.api.i18n.Lang

final class UblogPaginator(
    colls: UblogColls,
    relationApi: lila.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import UblogBsonHandlers.{ *, given }
  import UblogPost.PreviewPost

  val maxPerPage = MaxPerPage(9)

  def byUser(user: User, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    byBlog(UblogBlog.Id.User(user.id), live, page)

  def byBlog(blog: UblogBlog.Id, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new Adapter[PreviewPost](
        collection = colls.post,
        selector = $doc("blog" -> blog, "live" -> live),
        projection = previewPostProjection.some,
        sort = if (live) $doc("lived.at" -> -1) else $doc("created.at" -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByCommunity(lang: Option[Lang], page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost] {
        val select             = $doc("live" -> true) ++ lang.?? { l => $doc("language" -> l.code) }
        def nbResults: Fu[Int] = fuccess(10 * maxPerPage.value)
        def slice(offset: Int, length: Int) = aggregateVisiblePosts(select, offset, length)
      },
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByLiked(me: User, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new Adapter[PreviewPost](
        collection = colls.post,
        selector = $doc("live" -> true, "likers" -> me.id),
        projection = previewPostProjection.some,
        sort = $sort desc "rank",
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByTopic(topic: UblogTopic, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost] {
        def nbResults: Fu[Int] = fuccess(10 * maxPerPage.value)
        def slice(offset: Int, length: Int) =
          aggregateVisiblePosts($doc("topics" -> topic.value), offset, length)
      },
      currentPage = page,
      maxPerPage = maxPerPage
    )

  private def aggregateVisiblePosts(select: Bdoc, offset: Int, length: Int) = colls.post
    .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
      import framework.*
      Match(select ++ $doc("live" -> true)) -> List(
        Sort(Descending("rank")),
        PipelineOperator(
          $lookup.pipeline(
            from = colls.blog,
            as = "blog",
            local = "blog",
            foreign = "_id",
            pipe = List(
              $doc("$match"   -> $expr($doc("$gte" -> $arr("$tier", UblogBlog.Tier.LOW)))),
              $doc("$project" -> $id(true))
            )
          )
        ),
        UnwindField("blog"),
        Project(previewPostProjection ++ $doc("blog" -> "$blog._id")),
        Skip(offset),
        Limit(length)
      )
    }
    .map { docs =>
      for {
        doc  <- docs
        post <- doc.asOpt[PreviewPost]
      } yield post
    }

  object liveByFollowed:

    def apply(user: User, page: Int): Fu[Paginator[PreviewPost]] =
      Paginator(
        adapter = new AdapterLike[PreviewPost] {
          def nbResults: Fu[Int]              = fuccess(10 * maxPerPage.value)
          def slice(offset: Int, length: Int) = cache.get((user.id, offset, length))
        },
        currentPage = page,
        maxPerPage = maxPerPage
      )

    private val cache = cacheApi[(UserId, Int, Int), List[PreviewPost]](256, "ublog.paginator.followed")(
      _.expireAfterWrite(15 seconds)
        .buildAsyncFuture { case (userId, offset, length) =>
          relationApi.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework.*
              Match($doc("u1" -> userId, "r" -> lila.relation.Follow)) -> List(
                Group(BSONNull)("ids" -> PushField("u2")),
                PipelineOperator(
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
                ),
                Project($doc("ids" -> false, "_id" -> false)),
                UnwindField("post"),
                Limit(length),
                ReplaceRootField("post")
              )
            }
            .map { docs =>
              for {
                doc  <- docs
                post <- doc.asOpt[PreviewPost]
              } yield post
            }
        }
    )
