package lila.ublog

import akka.stream.scaladsl.*
import play.api.i18n.Lang
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*

import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline.{ Propagate, UblogPostLike }
import lila.user.{ Me, User }

final class UblogRank(
    colls: UblogColls,
    timeline: lila.hub.actors.Timeline
)(using Executor, akka.stream.Materializer):

  import UblogBsonHandlers.given

  private def selectLiker(userId: UserId) = $doc("likers" -> userId)

  def liked(post: UblogPost)(user: User): Fu[Boolean] =
    colls.post.exists($id(post.id) ++ selectLiker(user.id))

  def like(postId: UblogPostId, v: Boolean)(using me: Me): Fu[UblogPost.Likes] =
    colls.post.update
      .one(
        $id(postId),
        if v then $addToSet("likers" -> me.userId) else $pull("likers" -> me.userId)
      )
      .flatMap: res =>
        colls.post
          .aggregateOne(): framework =>
            import framework.*
            Match($id(postId)) -> List(
              PipelineOperator:
                $lookup.simple(from = colls.blog, as = "blog", local = "blog", foreign = "_id")
              ,
              UnwindField("blog"),
              Project(
                $doc(
                  "tier"     -> "$blog.tier",
                  "likes"    -> $doc("$size" -> "$likers"), // do not use denormalized field
                  "at"       -> "$lived.at",
                  "language" -> true,
                  "title"    -> true,
                  "imageId"  -> "$image.id"
                )
              )
            )
          .map: docOption =>
            for
              doc      <- docOption
              id       <- doc.getAsOpt[UblogPostId]("_id")
              likes    <- doc.getAsOpt[UblogPost.Likes]("likes")
              liveAt   <- doc.getAsOpt[Instant]("at")
              tier     <- doc.getAsOpt[UblogBlog.Tier]("tier")
              language <- doc.getAsOpt[Lang]("language")
              title    <- doc string "title"
              hasImage = doc.contains("imageId")
            yield (id, likes, liveAt, tier, language, title, hasImage)
          .flatMap:
            case None                                                       => fuccess(UblogPost.Likes(0))
            case Some((id, likes, liveAt, tier, language, title, hasImage)) =>
              // Multiple updates may race to set denormalized likes and rank,
              // but values should be approximately correct, match a real like
              // count (though perhaps not the latest one), and any uncontended
              // query will set the precisely correct value.
              colls.post.update.one(
                $id(postId),
                $set(
                  "likes" -> likes,
                  "rank"  -> computeRank(likes, liveAt, language, tier, hasImage)
                )
              ) andDo {
                if res.nModified > 0 && v && tier >= UblogBlog.Tier.LOW
                then timeline ! (Propagate(UblogPostLike(me, id.value, title)) toFollowersOf me)
              } inject likes

  def recomputeRankOfAllPostsOfBlog(blogId: UblogBlog.Id): Funit =
    colls.blog.byId[UblogBlog](blogId.full) flatMapz recomputeRankOfAllPostsOfBlog

  def recomputeRankOfAllPostsOfBlog(blog: UblogBlog): Funit =
    colls.post
      .find(
        $doc("blog" -> blog.id),
        $doc("likes" -> true, "lived" -> true, "language" -> true).some
      )
      .cursor[Bdoc](ReadPref.sec)
      .list(500)
      .flatMap:
        _.traverse_ : doc =>
          (
            doc.string("_id"),
            doc.getAsOpt[UblogPost.Likes]("likes"),
            doc.getAsOpt[UblogPost.Recorded]("lived"),
            doc.getAsOpt[Lang]("language"),
            doc.contains("image").some
          ).tupled so { (id, likes, lived, language, hasImage) =>
            colls.post
              .updateField($id(id), "rank", computeRank(likes, lived.at, language, blog.tier, hasImage))
              .void
          }

  def recomputeRankOfAllPosts: Funit =
    colls.blog
      .find($empty)
      .sort($sort desc "tier")
      .cursor[UblogBlog](ReadPref.sec)
      .documentSource()
      .mapAsyncUnordered(4)(recomputeRankOfAllPostsOfBlog)
      .runWith(lila.common.LilaStream.sinkCount)
      .map(nb => println(s"Recomputed rank of $nb blogs"))

  def computeRank(blog: UblogBlog, post: UblogPost): Option[UblogPost.RankDate] =
    post.lived.map: lived =>
      computeRank(post.likes, lived.at, post.language, blog.tier, post.image.nonEmpty)

  private def computeRank(
      likes: UblogPost.Likes,
      liveAt: Instant,
      language: Lang,
      tier: UblogBlog.Tier,
      hasImage: Boolean
  ) = UblogPost.RankDate {
    import UblogBlog.Tier.*
    if tier < LOW || !hasImage then liveAt minusMonths 3
    else
      liveAt plusHours:
        val tierBase = 24 * tier.match
          case LOW    => -30
          case NORMAL => 0
          case HIGH   => 10
          case BEST   => 15
          case _      => 0

        val likesBonus = math.sqrt(likes.value * 25) + likes.value / 100

        val langBonus = if language.language == lila.i18n.defaultLang.language then 0 else -24 * 10

        (tierBase + likesBonus + langBonus).toInt
  }
