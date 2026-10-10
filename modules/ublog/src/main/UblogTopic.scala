package lila.ublog

import scalalib.ThreadLocalRandom.shuffle
import lila.db.dsl.{ *, given }
import lila.core.ublog.Quality
import lila.core.i18n.I18nKey
import lila.memo.CacheApi
import lila.memo.CacheApi.buildAsyncTimeout

opaque type UblogTopic = String
object UblogTopic extends OpaqueString[UblogTopic]:
  extension (a: UblogTopic)
    def url = a.replace(" ", "_")
    def i18nKey: I18nKey = i18nKeys.getOrElse(a, I18nKey(a))

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
  val exists: Set[UblogTopic] = all.toSet
  val chessExists: Set[UblogTopic] = chess.toSet
  def get(str: String): Option[UblogTopic] = exists(str).option(UblogTopic(str))
  def fromStrList(str: String): List[UblogTopic] = str.split(',').toList.flatMap(get).distinct
  def fromUrl(str: String): Option[UblogTopic] = get(str.replace("_", " "))

  private lazy val i18nKeys: Map[UblogTopic, I18nKey] = all
    .zip:
      all.map:
        case "Analysis" => I18nKey.site.analysis
        case "Opening" => I18nKey.site.opening
        case "Endgame" => I18nKey.site.endgame
        case "Tournament" => I18nKey.site.tournament
        case t => I18nKey(s"ublog:topic${t.split(" ").map(_.capitalize).mkString}")
    .toMap

  case class WithPosts(topic: UblogTopic, posts: List[UblogPost.PreviewPost], nb: Int)

final class UblogTopicApi(colls: UblogColls, cacheApi: CacheApi)(using Executor, Scheduler):

  import UblogBsonHandlers.{ *, given }

  private val withPostsCache =
    cacheApi.unit[List[UblogTopic.WithPosts]]("ublog.topic.withPosts"):
      _.refreshAfterWrite(5.minutes).buildAsyncTimeout(): _ =>
        UblogTopic.all
          .map: topic =>
            for
              count <- colls.post.secondary.countSel:
                bdoc("live" -> true, "topics" -> topic, "automod.quality" -> neq(0))
              posts <- colls.post
                .find(
                  bdoc(
                    "live" -> true,
                    "topics" -> topic,
                    "automod.quality" -> gte(Quality.good.ordinal),
                    "likes" -> gt(50)
                  ),
                  previewPostProjection.some
                )
                .sort(bdoc("lived.at" -> -1))
                .cursor[UblogPost.PreviewPost](ReadPref.sec)
                .list(16)
            yield UblogTopic.WithPosts(topic, shuffle(posts).take(4), count)
          .parallel

  def withPosts: Fu[List[UblogTopic.WithPosts]] = withPostsCache.get {}
