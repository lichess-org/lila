package lila.ublog

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

opaque type UblogTopic = String
object UblogTopic extends OpaqueString[UblogTopic]:
  extension (a: UblogTopic) def url = a.replace(" ", "_")

  val chess: List[UblogTopic] = List(
    "Chess",
    "Analysis",
    "Puzzle",
    "Opening",
    "Endgame",
    "Tactics",
    "Strategy",
    "Chess engine",
    "Chess bot",
    "Chess Personalities",
    "Over the board",
    "Tournament",
    "Chess variant"
  )
  val offTopic: UblogTopic = "Off topic"
  val all: List[UblogTopic] = chess ::: List(
    "Software Development",
    "Lichess",
    offTopic
  )
  val exists: Set[UblogTopic]                    = all.toSet
  val chessExists: Set[UblogTopic]               = chess.toSet
  def get(str: String): Option[UblogTopic]       = exists(str).option(UblogTopic(str))
  def fromStrList(str: String): List[UblogTopic] = str.split(',').toList.flatMap(get).distinct
  def fromUrl(str: String): Option[UblogTopic]   = get(str.replace("_", " "))

  case class WithPosts(topic: UblogTopic, posts: List[UblogPost.PreviewPost], nb: Int)

final class UblogTopicApi(colls: UblogColls, cacheApi: CacheApi)(using Executor):

  import UblogBsonHandlers.{ *, given }

  private val withPostsCache =
    cacheApi.unit[List[UblogTopic.WithPosts]]:
      _.refreshAfterWrite(30 seconds).buildAsyncFuture: _ =>
        colls.post
          .aggregateList(UblogTopic.all.size, _.sec): framework =>
            import framework.*
            Facet(
              UblogTopic.all.map: topic =>
                topic.value -> List(
                  Match($doc("live" -> true, "topics" -> topic)),
                  Sort(Descending("rank")),
                  Project(previewPostProjection ++ $doc("rank" -> true)),
                  Group(BSONNull)("nb" -> SumAll, "posts" -> PushField("$ROOT")),
                  Project:
                    $doc(
                      "_id" -> false,
                      "nb"  -> true,
                      "posts" -> $doc(
                        "$filter" -> $doc(
                          "input" -> $doc("$slice" -> $arr("$posts", 4)),
                          "as"    -> "post",
                          "cond"  -> $doc("$gt" -> $arr("$$post.rank", nowInstant))
                        )
                      )
                    )
                )
            ) -> List(
              Project($doc("all" -> $doc("$objectToArray" -> "$$ROOT"))),
              UnwindField("all"),
              ReplaceRootField("all"),
              Unwind("v"),
              Project($doc("k" -> true, "nb" -> "$v.nb", "posts" -> "$v.posts"))
            )
          .map: docs =>
            for
              doc   <- docs
              t     <- doc.string("k")
              topic <- UblogTopic.get(t)
              nb    <- doc.int("nb")
              posts <- doc.getAsOpt[List[UblogPost.PreviewPost]]("posts")
            yield UblogTopic.WithPosts(topic, posts, nb)

  def withPosts: Fu[List[UblogTopic.WithPosts]] = withPostsCache.get {}
