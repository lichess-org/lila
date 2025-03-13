package lila.ublog

import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*

import chess.IntRating
import scalalib.model.Language
import lila.core.perf.UserWithPerfs
import lila.core.timeline.{ Propagate, UblogPostLike }
import lila.db.dsl.{ *, given }

object UblogRank:

  opaque type Tier = Int
  object Tier extends RelaxedOpaqueInt[Tier]:
    val HIDDEN: Tier  = 0 // not visible
    val VISIBLE: Tier = 1 // not listed in community page
    val LOW: Tier     = 2 // from here, ranking boost
    val NORMAL: Tier  = 3
    val HIGH: Tier    = 4
    val BEST: Tier    = 5

    def default(user: UserWithPerfs) =
      if user.marks.troll then Tier.HIDDEN
      else if user.hasTitle || user.perfs.standard.glicko.establishedIntRating.exists(_ > IntRating(2200))
      then Tier.NORMAL
      else Tier.LOW

    def defaultWithoutPerfs(user: User) =
      if user.marks.troll then Tier.HIDDEN
      else if user.hasTitle then Tier.NORMAL
      else Tier.LOW

    val options = List(
      HIDDEN  -> "Hidden",
      VISIBLE -> "Unlisted",
      LOW     -> "Low",
      NORMAL  -> "Normal",
      HIGH    -> "High",
      BEST    -> "Best"
    )
    object tierDays:
      val LOW  = -7
      val HIGH = 5
      val BEST = 7
      val map  = Map(Tier.LOW -> LOW, Tier.HIGH -> HIGH, Tier.BEST -> BEST)

    val verboseOptions = List(
      HIDDEN  -> "Hidden",
      VISIBLE -> "Unlisted",
      LOW     -> s"Low (${tierDays.LOW} day penalty)",
      NORMAL  -> "Normal",
      HIGH    -> s"High (${tierDays.HIGH} day bonus)",
      BEST    -> s"Best (${tierDays.BEST} day bonus)"
    )
    def name(tier: Tier) = options.collectFirst {
      case (t, n) if t == tier => n
    } | "???"

  def computeRank(
      likes: UblogPost.Likes,
      liveAt: Instant,
      language: Language,
      tier: Tier,
      hasImage: Boolean,
      days: Int
  ) = UblogPost.RankDate:
    import Tier.*
    liveAt
      .minusMonths(if tier < LOW || !hasImage then 3 else 0)
      .plusHours:
        val tierBase    = 24 * tierDays.map.getOrElse(tier, 0)
        val adjustBonus = 24 * days
        val likesBonus  = math.sqrt(likes.value * 25) + likes.value / 100
        val langBonus   = if language == lila.core.i18n.defaultLanguage then 0 else -24 * 10

        (tierBase + likesBonus + langBonus + adjustBonus).toInt

  // `byRank` by default takes into acount the date at which the post was published
  enum Type:
    case ByDate, ByRank, ByTimelessRank

    def sortingQuery(coll: Coll, framework: coll.AggregationFramework.type) =
      import framework.*
      this match
        case ByDate => List(Sort(Descending("lived.at")))
        case ByRank => List(Sort(Descending("rank")))
        case ByTimelessRank =>
          List(
            Project(
              $doc(
                "timelessRank" -> $doc("$subtract" -> $arr("$rank", "$lived.at"))
              ) ++ UblogBsonHandlers.previewPostProjection
            ),
            Sort(Descending("timelessRank"))
          )

final class UblogRank(colls: UblogColls)(using Executor, akka.stream.Materializer):

  import UblogBsonHandlers.given, UblogRank.Tier

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
                  "tier"           -> "$blog.tier",
                  "likes"          -> $doc("$size" -> "$likers"), // do not use denormalized field
                  "at"             -> "$lived.at",
                  "language"       -> true,
                  "title"          -> true,
                  "imageId"        -> "$image.id",
                  "rankAdjustDays" -> true
                )
              )
            )
          .map: docOption =>
            for
              doc      <- docOption
              id       <- doc.getAsOpt[UblogPostId]("_id")
              likes    <- doc.getAsOpt[UblogPost.Likes]("likes")
              liveAt   <- doc.getAsOpt[Instant]("at")
              tier     <- doc.getAsOpt[Tier]("tier")
              language <- doc.getAsOpt[Language]("language")
              title    <- doc.string("title")
              adjust   = ~doc.int("rankAdjustDays")
              hasImage = doc.contains("imageId")
            yield (id, likes, liveAt, tier, language, title, hasImage, adjust)
          .flatMap:
            case None => fuccess(UblogPost.Likes(0))
            case Some(id, likes, liveAt, tier, language, title, hasImage, adjust) =>
              // Multiple updates may race to set denormalized likes and rank,
              // but values should be approximately correct, match a real like
              // count (though perhaps not the latest one), and any uncontended
              // query will set the precisely correct value.
              for
                _ <- colls.post.update
                  .one(
                    $id(postId),
                    $set(
                      "likes" -> likes,
                      "rank"  -> UblogRank.computeRank(likes, liveAt, language, tier, hasImage, adjust)
                    )
                  )
                _ =
                  if res.nModified > 0 && v && tier >= Tier.LOW
                  then lila.common.Bus.pub(Propagate(UblogPostLike(me, id, title)).toFollowersOf(me))
              yield likes

  def recomputePostRank(post: UblogPost): Funit =
    recomputeRankOfAllPostsOfBlog(post.blog, post.id.some)

  def recomputeRankOfAllPostsOfBlog(blogId: UblogBlog.Id, only: Option[UblogPostId] = none): Funit =
    colls.blog.byId[UblogBlog](blogId.full).flatMapz(recomputeRankOfAllPostsOfBlog(_, only))

  def recomputeRankOfAllPostsOfBlog(blog: UblogBlog, only: Option[UblogPostId]): Funit =
    colls.post
      .find(
        $doc("blog" -> blog.id) ++ only.so($id),
        $doc(List("likes", "lived", "language", "rankAdjustDays", "image").map(_ -> BSONBoolean(true))).some
      )
      .cursor[Bdoc](ReadPref.sec)
      .list(500)
      .flatMap:
        _.sequentiallyVoid: doc =>
          ~(for
            id       <- doc.string("_id")
            likes    <- doc.getAsOpt[UblogPost.Likes]("likes")
            lived    <- doc.getAsOpt[UblogPost.Recorded]("lived")
            language <- doc.getAsOpt[Language]("language")
            hasImage = doc.contains("image")
            adjust   = ~doc.int("rankAdjustDays")
          yield colls.post
            .updateField(
              $id(id),
              "rank",
              UblogRank.computeRank(likes, lived.at, language, blog.tier, hasImage, adjust)
            )
            .void)

  def recomputeRankOfAllPosts: Funit =
    colls.blog
      .find($empty)
      .sort($sort.desc("tier"))
      .cursor[UblogBlog](ReadPref.sec)
      .documentSource()
      .mapAsyncUnordered(4)(recomputeRankOfAllPostsOfBlog(_, none))
      .runWith(lila.common.LilaStream.sinkCount)
      .map(nb => println(s"Recomputed rank of $nb blogs"))

  def computeRank(blog: UblogBlog, post: UblogPost): Option[UblogPost.RankDate] =
    post.lived.map: lived =>
      UblogRank.computeRank(
        post.likes,
        lived.at,
        post.language,
        blog.tier,
        post.image.nonEmpty,
        ~post.rankAdjustDays
      )
