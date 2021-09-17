package lila.ublog

import akka.stream.scaladsl._
import cats.implicits._
import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.i18n.Lang
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.hub.actorApi.timeline.{ Propagate, UblogPostLike }
import lila.memo.SettingStore
import lila.user.User

final class UblogRank(
    colls: UblogColls,
    timeline: lila.hub.actors.Timeline,
    factor: SettingStore[Float] @@ UblogRankFactor
)(implicit ec: ExecutionContext, mat: akka.stream.Materializer) {

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
    if (tier < UblogBlog.Tier.LOW) liveAt minusMonths 1
    else
      liveAt plusHours {

        val boostedLikes = likes.value.toFloat + ((tier - 2) * 10).atLeast(0) // initial boost

        val baseHours =
          if (boostedLikes < 1) 0
          else (3 * math.log(boostedLikes) + 1).toFloat.atMost(boostedLikes)

        val topicsMultiplier = topics.count(t => UblogTopic.chessExists(t.value)) match {
          case 0 => 0.3
          case 1 => 1
          case _ => 1.2
        }

        val langMultiplier = if (language.language == lila.i18n.defaultLang.language) 1 else 0.5

        val tierMultiplier = tier match {
          case UblogBlog.Tier.LOW    => 0.3
          case UblogBlog.Tier.NORMAL => 1
          case UblogBlog.Tier.HIGH   => 3
          case UblogBlog.Tier.BEST   => 7
          case _                     => 0
        }

        (baseHours * topicsMultiplier * langMultiplier * tierMultiplier * factor.get()).toInt
      }
  }
}
