package lila.app
package mashup

import chess.Color
import org.joda.time.Period

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.{ GameRepo, Game, Crosstable, TimeCount }
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.User

case class UserInfo(
    user: User,
    rank: Option[(Int, Int)],
    nbPlaying: Int,
    crosstable: Option[Crosstable],
    nbBookmark: Int,
    ratingChart: Option[String],
    nbFollowing: Int,
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int,
    totalTime: Period) {

  def nbRated = user.count.rated

  def nbWithMe = crosstable ?? (_.nbGames)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)
}

object UserInfo {

  def apply(
    countUsers: () => Fu[Int],
    bookmarkApi: BookmarkApi,
    relationApi: RelationApi,
    gameCached: lila.game.Cached,
    crosstableApi: lila.game.CrosstableApi,
    postApi: PostApi,
    getRatingChart: User => Fu[Option[String]],
    getRank: String => Fu[Option[Int]])(user: User, ctx: Context): Fu[UserInfo] =
    (getRank(user.id) flatMap {
      _ ?? { rank => countUsers() map { nb => (rank -> nb).some } }
    }) zip
      ((ctx is user) ?? { gameCached nbPlaying user.id map (_.some) }) zip
      (ctx.me.filter(user!=) ?? { me => crosstableApi(me.id, user.id) }) zip
      getRatingChart(user) zip
      relationApi.nbFollowing(user.id) zip
      relationApi.nbFollowers(user.id) zip
      ((ctx.me ?? Granter(_.UserSpy)) ?? {
        relationApi.nbBlockers(user.id) map (_.some)
      }) zip
      postApi.nbByUser(user.id) zip
      TimeCount.total(user.id) map {
        case ((((((((rank, nbPlaying), crosstable), ratingChart), nbFollowing), nbFollowers), nbBlockers), nbPosts), totalTime) => new UserInfo(
          user = user,
          rank = rank,
          nbPlaying = ~nbPlaying,
          crosstable = crosstable,
          nbBookmark = bookmarkApi countByUser user,
          ratingChart = ratingChart,
          nbFollowing = nbFollowing,
          nbFollowers = nbFollowers,
          nbBlockers = nbBlockers,
          nbPosts = nbPosts,
          totalTime = totalTime)
      }
}
