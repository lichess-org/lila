package lila.app
package mashup

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.{ User, Trophy, Trophies, TrophyApi }

case class UserInfo(
    user: User,
    ranks: lila.rating.UserRankMap,
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
    playTime: Option[User.PlayTime],
    trophies: Trophies,
    teamIds: List[String],
    isStreamer: Boolean,
    isCoach: Boolean,
    insightVisible: Boolean,
    completionRate: Option[Double]
) {

  def nbRated = user.count.rated

  def nbWithMe = crosstable ?? (_.nbGames)

  def percentRated: Int = math.round(nbRated / user.count.game.toFloat * 100)

  def completionRatePercent = completionRate.map { cr => math.round(cr * 100) }

  def isPublicMod = lila.security.Granter(_.PublicMod)(user)
  def isDeveloper = lila.security.Granter(_.Developer)(user)

  lazy val allTrophies = List(
    isPublicMod option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Moderator,
      date = org.joda.time.DateTime.now
    ),
    isDeveloper option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Developer,
      date = org.joda.time.DateTime.now
    ),
    isStreamer option Trophy(
      _id = "",
      user = user.id,
      kind = Trophy.Kind.Streamer,
      date = org.joda.time.DateTime.now
    )
  ).flatten ::: trophies

  def countTrophiesAndPerfCups = allTrophies.size + ranks.count(_._2 <= 100)
}

object UserInfo {

  def apply(
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
    fetchIsStreamer: String => Fu[Boolean],
    fetchTeamIds: User.ID => Fu[List[String]],
    fetchIsCoach: User => Fu[Boolean],
    insightShare: lila.insight.Share,
    getPlayTime: User => Fu[Option[User.PlayTime]],
    completionRate: User.ID => Fu[Option[Double]]
  )(user: User, ctx: Context): Fu[UserInfo] =
    getRanks(user.id).mon(_.http.response.user part "ranks") zip
      (gameCached nbPlaying user.id).mon(_.http.response.user part "nbPlaying") zip
      gameCached.nbImportedBy(user.id).mon(_.http.response.user part "nbImported") zip
      (ctx.me.filter(user!=) ?? { me => crosstableApi(me.id, user.id).mon(_.http.response.user part "crosstable") }) zip
      getRatingChart(user).mon(_.http.response.user part "ratingChart") zip
      relationApi.countFollowers(user.id).mon(_.http.response.user part "countFollowers") zip
      (ctx.me ?? Granter(_.UserSpy) ?? { relationApi.countBlockers(user.id) map (_.some) }) zip
      postApi.nbByUser(user.id).mon(_.http.response.user part "nbPosts") zip
      studyRepo.countByOwner(user.id).mon(_.http.response.user part "nbStudies") zip
      trophyApi.findByUser(user).mon(_.http.response.user part "trophies") zip
      fetchTeamIds(user.id).mon(_.http.response.user part "teams") zip
      fetchIsCoach(user).mon(_.http.response.user part "coach") zip
      fetchIsStreamer(user.id).mon(_.http.response.user part "streamer") zip
      (user.count.rated >= 10).?? {
        insightShare.grant(user, ctx.me).mon(_.http.response.user part "insight")
      } zip
      getPlayTime(user).mon(_.http.response.user part "playTime") zip
      completionRate(user.id).mon(_.http.response.user part "completionRate") zip
      bookmarkApi.countByUser(user).mon(_.http.response.user part "nbBookmarks") flatMap {
        case ranks ~ nbPlaying ~ nbImported ~ crosstable ~ ratingChart ~ nbFollowers ~ nbBlockers ~ nbPosts ~ nbStudies ~ trophies ~ teamIds ~ isCoach ~ isStreamer ~ insightVisible ~ playTime ~ completionRate ~ nbBookmarks =>
          (nbPlaying > 0) ?? {
            isHostingSimul(user.id).mon(_.http.response.user part "hasSimul")
          } map { hasSimul =>
            new UserInfo(
              user = user,
              ranks = ranks,
              nbPlaying = nbPlaying,
              hasSimul = hasSimul,
              crosstable = crosstable,
              nbBookmark = nbBookmarks,
              nbImported = nbImported,
              ratingChart = ratingChart,
              nbFollowers = nbFollowers,
              nbBlockers = nbBlockers,
              nbPosts = nbPosts,
              nbStudies = nbStudies,
              playTime = playTime,
              trophies = trophies,
              teamIds = teamIds,
              isStreamer = isStreamer,
              isCoach = isCoach,
              insightVisible = insightVisible,
              completionRate = completionRate
            )
          }
      }
}
