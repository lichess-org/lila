package lila.ublog

import cats.implicits._
import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.api._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.hub.actorApi.timeline.{ Propagate, UblogPostLike }
import lila.user.User

final class UblogRank(colls: UblogColls, timeline: lila.hub.actors.Timeline)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  private def selectLiker(userId: User.ID) = $doc("likers" -> userId)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    colls.post.exists($id(post.id) ++ selectLiker(user.id))

  def like(postId: UblogPost.Id, user: User, v: Boolean): Fu[UblogPost.Likes] =
    colls.post
      .aggregateOne() { framework =>
        import framework._
        Match($id(postId)) -> List(
          PipelineOperator($lookup.simple(from = colls.blog, as = "blog", local = "blog", foreign = "_id")),
          UnwindField("blog"),
          Project(
            $doc(
              "tier"     -> "$blog.tier",
              "likes"    -> $doc("$size" -> "$likers"),
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
          id       <- doc.getAsOpt[UblogPost.Id]("_id")
          likes    <- doc.getAsOpt[UblogPost.Likes]("likes")
          topics   <- doc.getAsOpt[List[UblogTopic]]("topics")
          liveAt   <- doc.getAsOpt[DateTime]("at")
          tier     <- doc int "tier"
          language <- doc.getAsOpt[Lang]("language")
          title    <- doc string "title"
        } yield (id, topics, likes, liveAt, tier, language, title)
      }
      .flatMap {
        _.fold(fuccess(UblogPost.Likes(v ?? 1))) {
          case (id, topics, prevLikes, liveAt, tier, language, title) =>
            val likes = UblogPost.Likes(prevLikes.value + (if (v) 1 else -1))
            colls.post.update.one(
              $id(postId),
              $set(
                "likes" -> likes,
                "rank"  -> computeRank(topics, likes, liveAt, language, tier)
              ) ++ {
                if (v) $addToSet("likers" -> user.id) else $pull("likers" -> user.id)
              }
            ) >>- {
              if (v && tier >= UblogBlog.Tier.NORMAL)
                timeline ! (Propagate(UblogPostLike(user.id, id.value, title)) toFollowersOf user.id)
            } inject likes
        }
      }

  def recomputeRankOfAllPosts(blogId: UblogBlog.Id): Funit =
    colls.blog.byId[UblogBlog](blogId.full) flatMap {
      _ ?? { blog =>
        colls.post
          .find(
            $doc("blog" -> blog.id),
            $doc("topics" -> true, "likes" -> true, "lived" -> true, "language" -> true).some
          )
          .cursor[Bdoc]()
          .list() flatMap { docs =>
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
      }
    }

  def computeRank(blog: UblogBlog, post: UblogPost): Option[UblogPost.Rank] =
    post.lived map { lived =>
      computeRank(post.topics, post.likes, lived.at, post.language, blog.tier)
    }

  private def computeRank(
      topics: List[UblogTopic],
      likes: UblogPost.Likes,
      liveAt: DateTime,
      language: Lang,
      tier: UblogBlog.Tier
  ) = UblogPost.Rank {
    liveAt plusHours {
      val tierLikes = likes.value + ((tier - 2) * 5).atLeast(0) // initial boost
      val likeHours =
        if (tierLikes < 1) 0
        else (5 * math.log(tierLikes) + 1).toInt.atMost(tierLikes) * 5
      val topicsMultiplier = topics.count(t => UblogTopic.chessExists(t.value)) match {
        case 0 => 0.3
        case 1 => 1
        case _ => 1.2
      }
      val langMultiplier = if (language.language == lila.i18n.defaultLang.language) 1 else 0.5
      val tiered = tier match {
        case UblogBlog.Tier.LOW    => likeHours * 0.3 * topicsMultiplier * langMultiplier
        case UblogBlog.Tier.NORMAL => likeHours * topicsMultiplier * langMultiplier
        case UblogBlog.Tier.HIGH   => likeHours * 3 * topicsMultiplier * langMultiplier
        case UblogBlog.Tier.BEST   => likeHours * 7 * topicsMultiplier * langMultiplier
        case _                     => -99999
      }
      tiered.toInt
    }
  }
}
