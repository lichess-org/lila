package lila.app
package mashup

import play.api.data.Form

import lila.api.Context
import lila.bookmark.BookmarkApi
import lila.forum.PostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.security.Granter
import lila.user.{ User, Trophy, Trophies, TrophyApi }

case class UserInfo(
    user: User,
    ranks: Option[lila.rating.UserRankMap],
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

  def completionRatePercent = completionRate.map { cr => math.round(cr * 100) }

  def countTrophiesAndPerfCups = trophies.size + ranks.??(_.count(_._2 <= 100))
}

object UserInfo {

  sealed abstract class Angle(val key: String)
  object Angle {
    case object Activity extends Angle("activity")
    case class Games(searchForm: Option[Form[_]]) extends Angle("games")
    case object Other extends Angle("other")
  }

  case class Social(
      relation: Option[lila.relation.Relation],
      notes: List[lila.user.Note],
      followable: Boolean,
      blocked: Boolean
  )

  object Social {
    def apply(
      relationApi: RelationApi,
      noteApi: lila.user.NoteApi,
      prefApi: lila.pref.PrefApi
    )(u: User, ctx: Context): Fu[Social] =
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

  object NbGames {
    def apply(
      bookmarkApi: BookmarkApi,
      gameCached: lila.game.Cached,
      crosstableApi: lila.game.CrosstableApi
    )(u: User, ctx: Context): Fu[NbGames] =
      (ctx.me.filter(u!=) ?? { me => crosstableApi.withMatchup(me.id, u.id) }) zip
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

  def apply(
    relationApi: RelationApi,
    trophyApi: TrophyApi,
    shieldApi: lila.tournament.TournamentShieldApi,
    revolutionApi: lila.tournament.RevolutionApi,
    postApi: PostApi,
    studyRepo: lila.study.StudyRepo,
    getRatingChart: User => Fu[Option[String]],
    getRanks: User.ID => Fu[Option[lila.rating.UserRankMap]],
    isHostingSimul: User.ID => Fu[Boolean],
    fetchIsStreamer: User => Fu[Boolean],
    fetchTeamIds: User.ID => Fu[List[String]],
    fetchIsCoach: User => Fu[Boolean],
    insightShare: lila.insight.Share,
    getPlayTime: User => Fu[Option[User.PlayTime]],
    completionRate: User.ID => Fu[Option[Double]]
  )(user: User, nbs: NbGames, ctx: Context): Fu[UserInfo] =
    getRanks(user.id) zip
      (ctx.noBlind ?? getRatingChart(user)) zip
      relationApi.countFollowers(user.id) zip
      (ctx.me ?? Granter(_.UserSpy) ?? { relationApi.countBlockers(user.id) map (_.some) }) zip
      postApi.nbByUser(user.id) zip
      studyRepo.countByOwner(user.id) zip
      trophyApi.findByUser(user) zip
      fuccess(trophyApi.roleBasedTrophies(
        user,
        Granter(_.PublicMod)(user),
        Granter(_.Developer)(user),
        Granter(_.Verified)(user)
      )) zip
      shieldApi.active(user) zip
      revolutionApi.active(user) zip
      fetchTeamIds(user.id) zip
      fetchIsCoach(user) zip
      fetchIsStreamer(user) zip
      (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
      getPlayTime(user) zip
      completionRate(user.id) flatMap {
        case ranks ~ ratingChart ~ nbFollowers ~ nbBlockers ~ nbPosts ~ nbStudies ~ trophies ~ roleTrophies ~ shields ~ revols ~ teamIds ~ isCoach ~ isStreamer ~ insightVisible ~ playTime ~ completionRate =>
          (nbs.playing > 0) ?? isHostingSimul(user.id) map { hasSimul =>
            new UserInfo(
              user = user,
              ranks = ranks,
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
