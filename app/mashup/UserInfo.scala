package lila.app
package mashup

import chess.Color
import org.joda.time.Period

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.{ GameRepo, Game, Crosstable, PlayTime }
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.{ User, Trophy, Trophies, TrophyApi }

case class UserInfo(
    user: User,
    ranks: lila.rating.UserRankMap,
    nbUsers: Int,
    nbPlaying: Int,
    hasSimul: Boolean,
    crosstable: Option[Crosstable],
    nbBookmark: Int,
    nbImported: Int,
    ratingChart: Option[String],
    nbFollowing: Int,
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int,
    playTime: User.PlayTime,
    donor: Boolean,
    trophies: Trophies,
    isStreamer: Boolean,
    insightVisible: Boolean) {

  def nbRated = user.count.rated

  def nbWithMe = crosstable ?? (_.nbGames)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)

  def allTrophies = List(
    donor option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Donor,
      date = org.joda.time.DateTime.now),
    isStreamer option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Streamer,
      date = org.joda.time.DateTime.now)
  ).flatten ::: trophies
}

object UserInfo {

  def apply(
    countUsers: () => Fu[Int],
    bookmarkApi: BookmarkApi,
    relationApi: RelationApi,
    trophyApi: TrophyApi,
    gameCached: lila.game.Cached,
    crosstableApi: lila.game.CrosstableApi,
    postApi: PostApi,
    getRatingChart: User => Fu[Option[String]],
    getRanks: String => Fu[Map[String, Int]],
    isDonor: String => Fu[Boolean],
    isHostingSimul: String => Fu[Boolean],
    isStreamer: String => Boolean,
    insightShare: lila.insight.Share,
    getPlayTime: User => Fu[User.PlayTime])(user: User, ctx: Context): Fu[UserInfo] =
    countUsers() zip
      getRanks(user.id) zip
      (gameCached nbPlaying user.id) zip
      gameCached.nbImportedBy(user.id) zip
      (ctx.me.filter(user!=) ?? { me => crosstableApi(me.id, user.id) }) zip
      getRatingChart(user) zip
      relationApi.countFollowing(user.id) zip
      relationApi.countFollowers(user.id) zip
      (ctx.me ?? Granter(_.UserSpy) ?? { relationApi.countBlockers(user.id) map (_.some) }) zip
      postApi.nbByUser(user.id) zip
      isDonor(user.id) zip
      trophyApi.findByUser(user) zip
      (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
      getPlayTime(user) flatMap {
        case (((((((((((((nbUsers, ranks), nbPlaying), nbImported), crosstable), ratingChart), nbFollowing), nbFollowers), nbBlockers), nbPosts), isDonor), trophies), insightVisible), playTime) =>
          (nbPlaying > 0) ?? isHostingSimul(user.id) map { hasSimul =>
            new UserInfo(
              user = user,
              ranks = ranks,
              nbUsers = nbUsers,
              nbPlaying = nbPlaying,
              hasSimul = hasSimul,
              crosstable = crosstable,
              nbBookmark = bookmarkApi countByUser user,
              nbImported = nbImported,
              ratingChart = ratingChart,
              nbFollowing = nbFollowing,
              nbFollowers = nbFollowers,
              nbBlockers = nbBlockers,
              nbPosts = nbPosts,
              playTime = playTime,
              donor = isDonor,
              trophies = trophies,
              isStreamer = isStreamer(user.id),
              insightVisible = insightVisible)
          }
      }
}
