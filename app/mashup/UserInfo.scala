package lila.app
package mashup

import play.api.data.Form

import lila.api.{ Context, UserApi }
import lila.bookmark.BookmarkApi
import lila.forum.ForumPostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.security.Granter
import lila.ublog.{ UblogApi, UblogPost }
import lila.user.User

case class UserInfo(
    user: User,
    trophies: UserApi.TrophiesAndAwards,
    hasSimul: Boolean,
    ratingChart: Option[String],
    nbs: UserInfo.NbGames,
    nbFollowers: Int,
    nbForumPosts: Int,
    ublog: Option[UblogPost.BlogPreview],
    nbStudies: Int,
    teamIds: List[lila.team.TeamId],
    isStreamer: Boolean,
    isCoach: Boolean,
    insightVisible: Boolean
):
  def ranks      = trophies.ranks
  def crosstable = nbs.crosstable

object UserInfo:

  sealed abstract class Angle(val key: String)
  object Angle:
    case object Activity                          extends Angle("activity")
    case class Games(searchForm: Option[Form[?]]) extends Angle("games")
    case object Other                             extends Angle("other")

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
  ):
    def apply(u: User, ctx: Context): Fu[Social] =
      ctx.userId.?? {
        relationApi.fetchRelation(_, u.id).mon(_.user segment "relation")
      } zip
        ctx.me.?? { me =>
          fetchNotes(u, me).mon(_.user segment "notes")
        } zip
        ctx.isAuth.?? {
          prefApi.followable(u.id).mon(_.user segment "followable")
        } zip
        ctx.userId.?? { myId =>
          relationApi.fetchBlocks(u.id, myId).mon(_.user segment "blocks")
        } dmap { case (((relation, notes), followable), blocked) =>
          Social(relation, notes, followable, blocked)
        }

    def fetchNotes(u: User, me: User) =
      noteApi.get(u, me, Granter(_.ModNote)(me)) dmap {
        _.filter { n =>
          (!n.dox || Granter(_.Admin)(me))
        }
      }

  case class NbGames(
      crosstable: Option[Crosstable.WithMatchup],
      playing: Int,
      imported: Int,
      bookmark: Int
  ):
    def withMe: Option[Int] = crosstable.map(_.crosstable.nbGames)

  final class NbGamesApi(
      bookmarkApi: BookmarkApi,
      gameCached: lila.game.Cached,
      crosstableApi: lila.game.CrosstableApi
  ):
    def apply(u: User, ctx: Context, withCrosstable: Boolean): Fu[NbGames] =
      (withCrosstable ?? ctx.me.filter(u.!=) ?? { me =>
        crosstableApi.withMatchup(me.id, u.id) dmap some
      }).mon(_.user segment "crosstable") zip
        gameCached.nbPlaying(u.id).mon(_.user segment "nbPlaying") zip
        gameCached.nbImportedBy(u.id).mon(_.user segment "nbImported") zip
        bookmarkApi.countByUser(u).mon(_.user segment "nbBookmarks") dmap {
          case (((crosstable, playing), imported), bookmark) =>
            NbGames(
              crosstable,
              playing = playing,
              imported = imported,
              bookmark = bookmark
            )
        }

  final class UserInfoApi(
      relationApi: RelationApi,
      postApi: ForumPostApi,
      ublogApi: UblogApi,
      studyRepo: lila.study.StudyRepo,
      ratingChartApi: lila.history.RatingChartApi,
      userApi: lila.api.UserApi,
      isHostingSimul: lila.round.IsSimulHost,
      streamerApi: lila.streamer.StreamerApi,
      teamApi: lila.team.TeamApi,
      teamCache: lila.team.Cached,
      coachApi: lila.coach.CoachApi,
      insightShare: lila.insight.Share
  )(using Executor):
    def apply(user: User, nbs: NbGames, ctx: Context, withUblog: Boolean = true): Fu[UserInfo] =
      ((ctx.noBlind && ctx.pref.showRatings) ?? ratingChartApi(user)).mon(_.user segment "ratingChart") zip
        relationApi.countFollowers(user.id).mon(_.user segment "nbFollowers") zip
        !(user.is(User.lichessId) || user.isBot) ??
        postApi.nbByUser(user.id).mon(_.user segment "nbForumPosts") zip
        (withUblog ?? ublogApi.userBlogPreviewFor(user, 3, ctx.me)) zip
        studyRepo.countByOwner(user.id).recoverDefault.mon(_.user segment "nbStudies") zip
        userApi.getTrophiesAndAwards(user).mon(_.user segment "trophies") zip
        teamApi.joinedTeamsOfUserAsSeenBy(user, ctx.me).mon(_.user segment "teamIds") zip
        coachApi.isListedCoach(user).mon(_.user segment "coach") zip
        streamerApi.isActualStreamer(user).mon(_.user segment "streamer") zip
        (user.count.rated >= 10).??(insightShare.grant(user, ctx.me)) zip
        (nbs.playing > 0) ?? isHostingSimul(user.id).mon(_.user segment "simul") map {
          // format: off
          case ((((((((((ratingChart, nbFollowers), nbForumPosts), ublog), nbStudies), trophiesAndAwards), teamIds), isCoach), isStreamer), insightVisible), hasSimul) =>
          // format: on
            new UserInfo(
              user = user,
              nbs = nbs,
              hasSimul = hasSimul,
              ratingChart = ratingChart,
              nbFollowers = nbFollowers,
              nbForumPosts = nbForumPosts,
              ublog = ublog,
              nbStudies = nbStudies,
              trophies = trophiesAndAwards,
              teamIds = teamIds,
              isStreamer = isStreamer,
              isCoach = isCoach,
              insightVisible = insightVisible
            )
        }

    def preloadTeams(info: UserInfo) = teamCache.nameCache.preloadMany(info.teamIds)
