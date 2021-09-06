package lila.ublog

import reactivemongo.api._
import scala.concurrent.ExecutionContext
import lila.db.dsl._
import lila.user.User
import org.joda.time.DateTime

final class UblogLike(coll: Coll)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  private def selectLiker(userId: User.ID) = $doc("likers" -> userId)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    coll.exists($id(post.id) ++ selectLiker(user.id))

  def apply(postId: UblogPost.Id, user: User, v: Boolean): Fu[UblogPost.Likes] =
    countLikes(postId).flatMap {
      case None => fuccess(UblogPost.Likes(0))
      case Some((prevLikes, liveAt)) =>
        val likes = UblogPost.Likes(prevLikes.value + (if (v) 1 else -1))
        coll.update.one(
          $id(postId),
          $set(
            "likes" -> likes,
            "rank"  -> UblogPost.Rank.compute(likes, liveAt)
          ) ++ {
            if (v) $addToSet("likers" -> user.id) else $pull("likers" -> user.id)
          }
        ) inject likes
    }

  private def countLikes(postId: UblogPost.Id): Fu[Option[(UblogPost.Likes, DateTime)]] =
    coll
      .aggregateWith[Bdoc]() { framework =>
        import framework._
        List(
          Match($id(postId)),
          Project(
            $doc(
              "_id"    -> false,
              "likes"  -> $doc("$size" -> "$likers"),
              "liveAt" -> true
            )
          )
        )
      }
      .headOption
      .map { docOption =>
        for {
          doc    <- docOption
          likes  <- doc.getAsOpt[UblogPost.Likes]("likes")
          liveAt <- doc.getAsOpt[DateTime]("liveAt")
        } yield likes -> liveAt
      }
}
