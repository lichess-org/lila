package lila.ublog

import reactivemongo.api.*

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.user.User
import reactivemongo.api.bson.BSONNull
import lila.user.Me
import lila.i18n.Language

final class UblogPaginator(
    colls: UblogColls,
    relationApi: lila.relation.RelationApi,
    userRepo: lila.user.UserRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import UblogBsonHandlers.{ *, given }
  import UblogPost.PreviewPost

  val maxPerPage = MaxPerPage(9)

  def byUser[U: UserIdOf](user: U, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    byBlog(UblogBlog.Id.User(user.id), live, page)

  def byBlog(blog: UblogBlog.Id, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = Adapter[PreviewPost](
        collection = colls.post,
        selector = $doc("blog" -> blog, "live" -> live),
        projection = previewPostProjection.some,
        sort = if live then $doc("lived.at" -> -1) else $doc("created.at" -> -1),
        _.sec
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByCommunity(language: Option[Language], page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new AdapterLike[PreviewPost]:
        val select = $doc("live" -> true, "topics" $ne UblogTopic.offTopic) ++ language.so: l =>
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
        sort = $sort desc "lived.at",
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

  // So far this only hits a prod index if $select contains `topics`, or if byDate is false
  // i.e. byDate can only be true if $select contains `topics`
  private def aggregateVisiblePosts(select: Bdoc, offset: Int, length: Int, byDate: Boolean = false) =
    colls.post
      .aggregateList(length, _.sec): framework =>
        import framework.*
        Match(select ++ $doc("live" -> true)) -> List(
          Sort(Descending(if byDate then "lived.at" else "rank")),
          Limit(500),
          PipelineOperator:
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
          ,
          UnwindField("blog"),
          PipelineOperator:
            $lookup.pipeline(
              from = userRepo.coll,
              as = "user",
              local = "created.by",
              foreign = "_id",
              pipe = List(
                $doc("$match"   -> $doc(User.BSONFields.enabled -> true)),
                $doc("$project" -> $id(true))
              )
            )
          ,
          UnwindField("user"),
          Project(previewPostProjection ++ $doc("blog" -> "$blog._id")),
          Skip(offset),
          Limit(length)
        )
      .map: docs =>
        for
          doc  <- docs
          post <- doc.asOpt[PreviewPost]
        yield post

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
      _.expireAfterWrite(15 seconds).buildAsyncFuture: (userId, offset, length) =>
        relationApi.coll
          .aggregateList(length, _.sec) { framework =>
            import framework.*
            Match($doc("u1" -> userId, "r" -> lila.relation.Follow)) -> List(
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
