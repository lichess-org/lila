package lila.app
package mashup

import chess.Color

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.{ GameRepo, Game }
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.{ User, UserRepo, Confrontation }

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    nbPlaying: Int,
    confrontation: Option[Confrontation],
    nbBookmark: Int,
    ratingChart: Option[String],
    nbFollowing: Int,
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int) {

  def nbRated = user.count.rated

  def nbWithMe = confrontation ?? (_.games)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)
}

object UserInfo {

  def apply(
    countUsers: () ⇒ Fu[Int],
    bookmarkApi: BookmarkApi,
    relationApi: RelationApi,
    gameCached: lila.game.Cached,
    postApi: PostApi,
    getRatingChart: User ⇒ Fu[Option[String]],
    getRank: String ⇒ Fu[Option[Int]])(user: User, ctx: Context): Fu[UserInfo] =
    (getRank(user.id) flatMap {
      _ ?? { rank ⇒ countUsers() map { nb ⇒ (rank -> nb).some } }
    }) zip
      ((ctx is user) ?? {
        gameCached nbPlaying user.id map (_.some)
      }) zip
      (ctx.me.filter(user!=) ?? { me ⇒
        gameCached.confrontation(me, user) map (_.some filterNot (_.empty))
      }) zip
      (bookmarkApi countByUser user) zip
      getRatingChart(user) zip
      relationApi.nbFollowing(user.id) zip
      relationApi.nbFollowers(user.id) zip
      ((ctx.me ?? Granter(_.UserSpy)) ?? {
        relationApi.nbBlockers(user.id) map (_.some)
      }) zip
      postApi.nbByUser(user.id) map {
        case ((((((((rank, nbPlaying), confrontation), nbBookmark), ratingChart), nbFollowing), nbFollowers), nbBlockers), nbPosts) ⇒ new UserInfo(
          user = user,
          rank = rank,
          nbPlaying = ~nbPlaying,
          confrontation = confrontation,
          nbBookmark = nbBookmark,
          ratingChart = ratingChart,
          nbFollowing = nbFollowing,
          nbFollowers = nbFollowers,
          nbBlockers = nbBlockers,
          nbPosts = nbPosts)
      }
}
