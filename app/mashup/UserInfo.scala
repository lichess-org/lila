package lila.app
package mashup

import play.api.data.Form

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.{ Trophies, TrophyApi, User }

case class UserInfo(
    user: User,
    ranks: lila.rating.UserRankMap,
    hasSimul: Boolean,
    ratingChart: Option[String],
    nbs: UserInfo.NbGames,
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int,
    nbStudies: Int,
    playTime: Option[User.PlayTime],
    trophies: Trophies,
    shields: List[lila.tournament.TournamentShield.Award],
    revolutions: List[lila.tournament.Revolution.Award],
    teamIds: List[String],
    isStreamer: Boolean,
    isCoach: Boolean,
    insightVisible: Boolean,
    completionRate: Option[Double]
) {

  def crosstable = nbs.crosstable

  def completionRatePercent = completionRate.map { cr =>
    math.round(cr * 100)
  }

  def countTrophiesAndPerfCups = trophies.size + ranks.count(_._2 <= 100)
}

object UserInfo {

  sealed abstract class Angle(val key: String)
  object Angle {
    case object Activity                          extends Angle("activity")
    case class Games(searchForm: Option[Form[_]]) extends Angle("games")
    case object Other                             extends Angle("other")
  }

  case class Social(
      relation: Option[lila.relation.Relation],
      notes: List[lila.user.Note],
      followable: Boolean,
      blocked: Boolean
  )

  final class SocialApi(
      relationApi: RelationApi,
      noteApi: lila.user.NoteApi,
      prefApi: lila.pref.PrefApi
  )(implicit ec: scala.concurrent.ExecutionContext) {
    def apply(u: User, ctx: Context): Fu[Social] =
      ctx.userId.?? { relationApi.fetchRelation(_, u.id) } zip
        ctx.me.?? { me =>
          relationApi fetchFriends me.id flatMap { noteApi.get(u, me, _, ctx.me ?? Granter(_.ModNote)) }
        } zip
        ctx.isAuth.?? { prefApi followable u.id } zip
        ctx.userId.?? { relationApi.fetchBlocks(u.id, _) } map {
        case relation ~ notes ~ followable ~ blocked =>
          Social(relation, notes, followable, blocked)
      }
  }

  case class NbGames(
      crosstable: Option[Crosstable.WithMatchup],
      playing: Int,
      imported: Int,
      bookmark: Int
  ) {
    def withMe: Option[Int] = crosstable.map(_.crosstable.nbGames)
  }

  final class NbGamesApi(
      bookmarkApi: BookmarkApi,
      gameCached: lila.game.Cached,
      crosstableApi: lila.game.CrosstableApi
  )(implicit ec: scala.concurrent.ExecutionContext) {
    def apply(u: User, ctx: Context): Fu[NbGames] =
      (ctx.me.filter(u !=) ?? { me =>
        crosstableApi.withMatchup(me.id, u.id) dmap some
      }) zip
        gameCached.nbPlaying(u.id) zip
        gameCached.nbImportedBy(u.id) zip
        bookmarkApi.countByUser(u) map {
        case crosstable ~ playing ~ imported ~ bookmark =>
          NbGames(
            crosstable,
            playing = playing,
            imported = imported,
            bookmark = bookmark
          )
      }
  }

  final class UserInfoApi(
      relationApi: RelationApi,
      trophyApi: TrophyApi,
      shieldApi: lila.tournament.TournamentShieldApi,
      revolutionApi: lila.tournament.RevolutionApi,
      postApi: PostApi,
      studyRepo: lila.study.StudyRepo,
      ratingChartApi: lila.history.RatingChartApi,
      userCached: lila.user.Cached,
      isHostingSimul: lila.round.IsSimulHost,
      streamerApi: lila.streamer.StreamerApi,
      teamCached: lila.team.Cached,
      coachApi: lila.coach.CoachApi,
      insightShare: lila.insight.Share,
      playTimeApi: lila.game.PlayTimeApi,
      playbanApi: lila.playban.PlaybanApi
  )(implicit ec: scala.concurrent.ExecutionContext) {
    def apply(user: User, nbs: NbGames, ctx: Context): Fu[UserInfo] =
      (ctx.noBlind ?? ratingChartApi(user)) zip
        relationApi.countFollowers(user.id) zip
        (ctx.me ?? Granter(_.UserSpy) ?? { relationApi.countBlockers(user.id) dmap some }) zip
        postApi.nbByUser(user.id) zip
        studyRepo.countByOwner(user.id) zip
        trophyApi.findByUser(user) zip
        fuccess(
          trophyApi.roleBasedTrophies(
            user,
            Granter(_.PublicMod)(user),
            Granter(_.Developer)(user),
            Granter(_.Verified)(user)
          )
        ) zip
        shieldApi.active(user) zip
        revolutionApi.active(user) zip
        teamCached.teamIdsList(user.id) zip
        coachApi.isListedCoach(user) zip
        streamerApi.isStreamer(user) zip
        (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
        playTimeApi(user) zip
        playbanApi.completionRate(user.id) flatMap {
        case ratingChart ~ nbFollowers ~ nbBlockers ~ nbPosts ~ nbStudies ~ trophies ~ roleTrophies ~ shields ~ revols ~ teamIds ~ isCoach ~ isStreamer ~ insightVisible ~ playTime ~ completionRate =>
          (nbs.playing > 0) ?? isHostingSimul(user.id) map { hasSimul =>
            new UserInfo(
              user = user,
              ranks = userCached.rankingsOf(user.id),
              nbs = nbs,
              hasSimul = hasSimul,
              ratingChart = ratingChart,
              nbFollowers = nbFollowers,
              nbBlockers = nbBlockers,
              nbPosts = nbPosts,
              nbStudies = nbStudies,
              playTime = playTime,
              trophies = trophies ::: roleTrophies,
              shields = shields,
              revolutions = revols,
              teamIds = teamIds,
              isStreamer = isStreamer,
              isCoach = isCoach,
              insightVisible = insightVisible,
              completionRate = completionRate
            )
          }
      }
  }
}
