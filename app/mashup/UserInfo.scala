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
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int,
    nbStudies: Int,
    playTime: User.PlayTime,
    trophies: Trophies,
    isStreamer: Boolean,
    insightVisible: Boolean) {

  def nbRated = user.count.rated

  def nbWithMe = crosstable ?? (_.nbGames)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)

  def allTrophies = List(
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
    studyRepo: lila.study.StudyRepo,
    getRatingChart: User => Fu[Option[String]],
    getRanks: String => Fu[Map[String, Int]],
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
      relationApi.countFollowers(user.id) zip
      (ctx.me ?? Granter(_.UserSpy) ?? { relationApi.countBlockers(user.id) map (_.some) }) zip
      postApi.nbByUser(user.id) zip
      studyRepo.countByOwner(user.id) zip
      trophyApi.findByUser(user) zip
      (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
      getPlayTime(user) flatMap {
        case ((((((((((((nbUsers, ranks), nbPlaying), nbImported), crosstable), ratingChart), nbFollowers), nbBlockers), nbPosts), nbStudies), trophies), insightVisible), playTime) =>
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
              nbFollowers = nbFollowers,
              nbBlockers = nbBlockers,
              nbPosts = nbPosts,
              nbStudies = nbStudies,
              playTime = playTime,
              trophies = trophies,
              isStreamer = isStreamer(user.id),
              insightVisible = insightVisible)
          }
      }
}
