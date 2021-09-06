package lila.ublog

import scala.concurrent.duration._
import reactivemongo.api._
import scala.concurrent.ExecutionContext

import lila.common.config.MaxPerPage
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User
import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull

final class UblogPaginator(
    coll: Coll,
    relationApi: lila.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._
  import UblogPost.PreviewPost

  val maxPerPage = MaxPerPage(8)

  def byUser(user: User, live: Boolean, page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new Adapter[PreviewPost](
        collection = coll,
        selector = $doc("user" -> user.id, "live" -> live),
        projection = previewPostProjection.some,
        sort = if (live) $doc("liveAt" -> -1) else $doc("createdAt" -> -1),
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def liveByCommunity(page: Int): Fu[Paginator[PreviewPost]] =
    Paginator(
      adapter = new Adapter[PreviewPost](
        collection = coll,
        selector = $doc("live" -> true),
        projection = previewPostProjection.some,
        sort = $sort desc "rank",
        readPreference = ReadPreference.secondaryPreferred
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  object liveByFollowed {

    def apply(user: User, page: Int): Fu[Paginator[PreviewPost]] =
      Paginator(
        adapter = new AdapterLike[PreviewPost] {
          def nbResults: Fu[Int]              = fuccess(10 * maxPerPage.value)
          def slice(offset: Int, length: Int) = cache.get((user.id, offset, length))
        },
        currentPage = page,
        maxPerPage = maxPerPage
      )

    private val cache = cacheApi[(User.ID, Int, Int), List[PreviewPost]](256, "ublog.paginator.followed")(
      _.expireAfterWrite(15 seconds)
        .buildAsyncFuture { case (userId, offset, length) =>
          relationApi.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match($doc("u1" -> userId, "r" -> lila.relation.Follow)) -> List(
                Group(BSONNull)("ids" -> PushField("u2")),
                PipelineOperator(
                  $doc(
                    "$lookup" -> $doc(
                      "from" -> coll.name,
                      "as"   -> "post",
                      "let"  -> $doc("users" -> "$ids"),
                      "pipeline" -> $arr(
                        $doc(
                          "$match" -> $doc(
                            "$expr" -> $doc(
                              "$and" -> $arr(
                                $doc("$in" -> $arr(s"$$user", "$$users")),
                                $doc("$eq" -> $arr("$live", true)),
                                $doc("$gt" -> $arr("$liveAt", DateTime.now.minusMonths(3)))
                              )
                            )
                          )
                        ),
                        $doc("$project" -> previewPostProjection),
                        $doc("$sort"    -> $doc("liveAt" -> -1)),
                        $doc("$skip"    -> offset),
                        $doc("$limit"   -> length)
                      )
                    )
                  )
                ),
                Project($doc("ids" -> false, "_id" -> false)),
                UnwindField("post"),
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
  }
}
