package lidraughts.app
package mashup

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.bookmark.BookmarkApi
import lidraughts.forum.PostApi
import lidraughts.game.Crosstable
import lidraughts.relation.RelationApi
import lidraughts.security.Granter
import lidraughts.user.{ User, Trophy, Trophies, TrophyApi }

case class UserInfo(
    user: User,
    ranks: lidraughts.rating.UserRankMap,
    hasSimul: Boolean,
    ratingChart: Option[String],
    nbs: UserInfo.NbGames,
    nbFollowers: Int,
    nbBlockers: Option[Int],
    nbPosts: Int,
    nbStudies: Int,
    playTime: Option[User.PlayTime],
    trophies: Trophies,
    shields: List[lidraughts.tournament.TournamentShield.Award],
    revolutions: List[lidraughts.tournament.Revolution.Award],
    teamIds: List[String],
    isStreamer: Boolean,
    isCoach: Boolean,
    insightVisible: Boolean,
    completionRate: Option[Double]
) {

  def crosstable = nbs.crosstable

  def completionRatePercent = completionRate.map { cr => math.round(cr * 100) }

  def countTrophiesAndPerfCups = trophies.size + ranks.count(_._2 <= 100)
}

object UserInfo {

  sealed abstract class Angle(val key: String)
  object Angle {
    case object Activity extends Angle("activity")
    case class Games(searchForm: Option[Form[_]]) extends Angle("games")
    case object Other extends Angle("other")
  }

  case class Social(
      relation: Option[lidraughts.relation.Relation],
      notes: List[lidraughts.user.Note],
      followable: Boolean,
      blocked: Boolean
  )

  object Social {
    def apply(
      relationApi: RelationApi,
      noteApi: lidraughts.user.NoteApi,
      prefApi: lidraughts.pref.PrefApi
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
      gameCached: lidraughts.game.Cached,
      crosstableApi: lidraughts.game.CrosstableApi
    )(u: User, ctx: Context): Fu[NbGames] =
      (ctx.me.filter(u!=) ?? { me => crosstableApi.withMatchup(me.id, u.id) map some }) zip
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
    shieldApi: lidraughts.tournament.TournamentShieldApi,
    revolutionApi: lidraughts.tournament.RevolutionApi,
    postApi: PostApi,
    studyRepo: lidraughts.study.StudyRepo,
    getRatingChart: User => Fu[Option[String]],
    getRanks: User.ID => lidraughts.rating.UserRankMap,
    isHostingSimul: User.ID => Fu[Boolean],
    fetchIsStreamer: User => Fu[Boolean],
    fetchTeamIds: User.ID => Fu[List[String]],
    insightShare: lidraughts.insight.Share,
    getPlayTime: User => Fu[Option[User.PlayTime]],
    completionRate: User.ID => Fu[Option[Double]]
  )(user: User, nbs: NbGames, ctx: Context): Fu[UserInfo] =
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
      fetchIsStreamer(user) zip
      (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
      getPlayTime(user) zip
      completionRate(user.id) flatMap {
        case ratingChart ~ nbFollowers ~ nbBlockers ~ nbPosts ~ nbStudies ~ trophies ~ roleTrophies ~ shields ~ revols ~ teamIds ~ isStreamer ~ insightVisible ~ playTime ~ completionRate =>
          (nbs.playing > 0) ?? isHostingSimul(user.id) map { hasSimul =>
            new UserInfo(
              user = user,
              ranks = getRanks(user.id),
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
              isCoach = false,
              insightVisible = insightVisible,
              completionRate = completionRate
            )
          }
      }
}
