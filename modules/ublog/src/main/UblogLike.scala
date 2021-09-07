package lila.ublog

import reactivemongo.api._
import scala.concurrent.ExecutionContext
import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

final class UblogLike(colls: UblogColls)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  private def selectLiker(userId: User.ID) = $doc("likers" -> userId)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    colls.post.exists($id(post.id) ++ selectLiker(user.id))

  def apply(postId: UblogPost.Id, user: User, v: Boolean): Fu[UblogPost.Likes] =
    countLikes(postId).flatMap {
      case None => fuccess(UblogPost.Likes(v ?? 1))
      case Some((blog, prevLikes, liveAt)) =>
        val likes = UblogPost.Likes(prevLikes.value + (if (v) 1 else -1))
        colls.post.update.one(
          $id(postId),
          $set(
            "likes" -> likes,
            "rank"  -> computeRank(blog, likes, liveAt)
          ) ++ {
            if (v) $addToSet("likers" -> user.id) else $pull("likers" -> user.id)
          }
        ) inject likes
    }

  private def computeRank(blog: UblogBlog, likes: UblogPost.Likes, liveAt: DateTime) =
    UblogPost.Rank {
      liveAt plusHours {
        val baseHours = likesToHours(likes)
        blog.tier match {
          case UblogBlog.Tier.LOW    => (baseHours * 0.2).toInt
          case UblogBlog.Tier.NORMAL => baseHours
          case UblogBlog.Tier.HIGH   => baseHours * 5
          case UblogBlog.Tier.BEST   => baseHours * 12
          case _                     => -99999
        }
      }
    }

  private def likesToHours(likes: UblogPost.Likes): Int =
    if (likes.value < 1) 0
    else (5 * math.log(likes.value) + 1).toInt.min(likes.value) * 24

  private def countLikes(postId: UblogPost.Id): Fu[Option[(UblogBlog, UblogPost.Likes, DateTime)]] =
    colls.post
      .aggregateWith[Bdoc]() { framework =>
        import framework._
        List(
          Match($id(postId)),
          PipelineOperator($lookup.simple(colls.blog, "blog", "blog", "_id")),
          Project(
            $doc(
              "_id"      -> false,
              "blog"     -> true,
              "likes"    -> $doc("$size" -> "$likers"),
              "lived.at" -> true
            )
          )
        )
      }
      .headOption
      .map { docOption =>
        for {
          doc    <- docOption
          likes  <- doc.getAsOpt[UblogPost.Likes]("likes")
          lived  <- doc.getAsOpt[Bdoc]("lived")
          liveAt <- doc.getAsOpt[DateTime]("at")
          blogs  <- doc.getAsOpt[List[UblogBlog]]("blog")
          blog   <- blogs.headOption
        } yield (blog, likes, liveAt)
      }
}
