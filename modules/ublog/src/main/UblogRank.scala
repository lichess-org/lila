package lila.ublog

import cats.implicits._
import reactivemongo.api._
import scala.concurrent.ExecutionContext
import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

final class UblogRank(colls: UblogColls)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  private def selectLiker(userId: User.ID) = $doc("likers" -> userId)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    colls.post.exists($id(post.id) ++ selectLiker(user.id))

  def like(postId: UblogPost.Id, user: User, v: Boolean): Fu[UblogPost.Likes] =
    colls.post
      .aggregateOne() { framework =>
        import framework._
        Match($id(postId)) -> List(
          PipelineOperator($lookup.simple(colls.blog, "blog", "blog", "_id")),
          UnwindField("blog"),
          Project(
            $doc(
              "_id"    -> false,
              "tier"   -> "$blog.tier",
              "likes"  -> $doc("$size" -> "$likers"),
              "topics" -> "$topics",
              "at"     -> "$lived.at"
            )
          )
        )
      }
      .map { docOption =>
        for {
          doc    <- docOption
          likes  <- doc.getAsOpt[UblogPost.Likes]("likes")
          topics <- doc.getAsOpt[List[UblogPost.Topic]]("topics")
          liveAt <- doc.getAsOpt[DateTime]("at")
          tier   <- doc int "tier"
        } yield (topics, likes, liveAt, tier)
      }
      .flatMap {
        _.fold(fuccess(UblogPost.Likes(v ?? 1))) { case (topics, prevLikes, liveAt, tier) =>
          val likes = UblogPost.Likes(prevLikes.value + (if (v) 1 else -1))
          colls.post.update.one(
            $id(postId),
            $set(
              "likes" -> likes,
              "rank"  -> computeRank(topics, likes, liveAt, tier)
            ) ++ {
              if (v) $addToSet("likers" -> user.id) else $pull("likers" -> user.id)
            }
          ) inject likes
        }
      }

  def recomputeRankOfAllPosts(blogId: UblogBlog.Id): Funit =
    colls.blog.byId[UblogBlog](blogId.full) flatMap {
      _ ?? { blog =>
        colls.post
          .find($doc("blog" -> blog.id), $doc("topics" -> true, "likes" -> true, "lived" -> true).some)
          .cursor[Bdoc]()
          .list() flatMap { docs =>
          lila.common.Future.applySequentially(docs) { doc =>
            (
              doc.string("_id"),
              doc.getAsOpt[List[UblogPost.Topic]]("topics"),
              doc.getAsOpt[UblogPost.Likes]("likes"),
              doc.getAsOpt[UblogPost.Recorded]("lived")
            ).tupled ?? { case (id, topics, likes, lived) =>
              colls.post.updateField($id(id), "rank", computeRank(topics, likes, lived.at, blog.tier)).void
            }
          }
        }
      }
    }

  def computeRank(blog: UblogBlog, post: UblogPost): Option[UblogPost.Rank] =
    post.lived map { lived =>
      computeRank(post.topics, post.likes, lived.at, blog.tier)
    }

  private def computeRank(
      topics: List[UblogPost.Topic],
      likes: UblogPost.Likes,
      liveAt: DateTime,
      tier: UblogBlog.Tier
  ) = UblogPost.Rank {
    liveAt plusHours {
      val likeHours =
        if (likes.value < 1) 0
        else (5 * math.log(likes.value) + 1).toInt.atMost(likes.value) * 12
      val topicsMultiplier = topics.count(t => UblogPost.Topic.indexableExists(t.value)) match {
        case 0 => 0.5
        case 1 => 1
        case _ => 1.5
      }
      val tiered = tier match {
        case UblogBlog.Tier.LOW    => likeHours * 0.3 * topicsMultiplier
        case UblogBlog.Tier.NORMAL => likeHours * topicsMultiplier
        case UblogBlog.Tier.HIGH   => likeHours * 3 * topicsMultiplier
        case UblogBlog.Tier.BEST   => likeHours * 10 * topicsMultiplier
        case _                     => -99999
      }
      tiered.toInt
    }
  }
}
