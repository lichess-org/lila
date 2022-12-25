package lila.ublog

import akka.stream.scaladsl.*
import cats.implicits.*
import com.softwaremill.tagging.*
import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import scala.concurrent.ExecutionContext

import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline.{ Propagate, UblogPostLike }
import lila.memo.SettingStore
import lila.user.User

final class UblogRank(
    colls: UblogColls,
    timeline: lila.hub.actors.Timeline
)(using ec: ExecutionContext, mat: akka.stream.Materializer):

  import UblogBsonHandlers.given

  private def selectLiker(userId: UserId) = $doc("likers" -> userId)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    colls.post.exists($id(post.id) ++ selectLiker(user.id))

  def like(postId: UblogPostId, user: User, v: Boolean): Fu[UblogPost.Likes] =
    colls.post.update.one(
      $id(postId),
      if (v) $addToSet("likers" -> user.id) else $pull("likers" -> user.id)
    ) flatMap { res =>
      colls.post
        .aggregateOne() { framework =>
          import framework.*
          Match($id(postId)) -> List(
            PipelineOperator($lookup.simple(from = colls.blog, as = "blog", local = "blog", foreign = "_id")),
            UnwindField("blog"),
            Project(
              $doc(
                "tier"     -> "$blog.tier",
                "likes"    -> $doc("$size" -> "$likers"), // do not use denormalized field
                "topics"   -> "$topics",
                "at"       -> "$lived.at",
                "language" -> true,
                "title"    -> true
              )
            )
          )
        }
        .map { docOption =>
          for {
            doc      <- docOption
            id       <- doc.getAsOpt[UblogPostId]("_id")
            likes    <- doc.getAsOpt[UblogPost.Likes]("likes")
            topics   <- doc.getAsOpt[List[UblogTopic]]("topics")
            liveAt   <- doc.getAsOpt[DateTime]("at")
            tier     <- doc int "tier"
            language <- doc.getAsOpt[Lang]("language")
            title    <- doc string "title"
          } yield (id, topics, likes, liveAt, tier, language, title)
        }
        .flatMap {
          case None                                                     => fuccess(UblogPost.Likes(0))
          case Some((id, topics, likes, liveAt, tier, language, title)) =>
            // Multiple updates may race to set denormalized likes and rank,
            // but values should be approximately correct, match a real like
            // count (though perhaps not the latest one), and any uncontended
            // query will set the precisely correct value.
            colls.post.update.one(
              $id(postId),
              $set(
                "likes" -> likes,
                "rank"  -> computeRank(topics, likes, liveAt, language, tier)
              )
            ) >>- {
              if (res.nModified > 0 && v && tier >= UblogBlog.Tier.LOW)
                timeline ! (Propagate(UblogPostLike(user.id, id.value, title)) toFollowersOf user.id)
            } inject likes
        }
    }

  def recomputeRankOfAllPostsOfBlog(blogId: UblogBlog.Id): Funit =
    colls.blog.byId[UblogBlog](blogId.full) flatMap {
      _ ?? recomputeRankOfAllPostsOfBlog
    }

  def recomputeRankOfAllPostsOfBlog(blog: UblogBlog): Funit =
    colls.post
      .find(
        $doc("blog" -> blog.id),
        $doc("topics" -> true, "likes" -> true, "lived" -> true, "language" -> true).some
      )
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .list(500) flatMap { docs =>
      lila.common.Future.applySequentially(docs) { doc =>
        (
          doc.string("_id"),
          doc.getAsOpt[List[UblogTopic]]("topics"),
          doc.getAsOpt[UblogPost.Likes]("likes"),
          doc.getAsOpt[UblogPost.Recorded]("lived"),
          doc.getAsOpt[Lang]("language")
        ).tupled ?? { case (id, topics, likes, lived, language) =>
          colls.post
            .updateField($id(id), "rank", computeRank(topics, likes, lived.at, language, blog.tier))
            .void
        }
      }
    }

  def recomputeRankOfAllPosts: Funit =
    colls.blog
      .find($empty)
      .sort($sort desc "tier")
      .cursor[UblogBlog](ReadPreference.secondaryPreferred)
      .documentSource()
      .mapAsyncUnordered(4)(recomputeRankOfAllPostsOfBlog)
      .toMat(lila.common.LilaStream.sinkCount)(Keep.right)
      .run()
      .map(nb => println(s"Recomputed rank of $nb blogs"))

  def computeRank(blog: UblogBlog, post: UblogPost): Option[UblogPost.RankDate] =
    post.lived map { lived =>
      computeRank(post.topics, post.likes, lived.at, post.language, blog.tier)
    }

  private def computeRank(
      topics: List[UblogTopic],
      likes: UblogPost.Likes,
      liveAt: DateTime,
      language: Lang,
      tier: UblogBlog.Tier
  ) = UblogPost.RankDate {
    import UblogBlog.Tier.*
    if (tier < LOW) liveAt minusMonths 3
    else
      liveAt plusHours {

        val tierBase = 24 * (tier match {
          case LOW    => -30
          case NORMAL => 0
          case HIGH   => 10
          case BEST   => 15
          case _      => 0
        })

        val likesBonus = math.sqrt(likes.value * 25) + likes.value / 100

        val topicsBonus = if (topics.exists(t => UblogTopic.chessExists(t.value))) 0 else -24 * 5

        val langBonus = if (language.language == lila.i18n.defaultLang.language) 0 else -24 * 10

        (tierBase + likesBonus + topicsBonus + langBonus).toInt
      }
  }
