package lila.app
package mashup

import play.api.data.Form

import lila.bookmark.BookmarkApi
import lila.forum.ForumPostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.security.Granter
import lila.ublog.{ UblogApi, UblogPost }
import lila.user.{ Me, User }

case class UserInfo(
    user: User,
    trophies: lila.api.UserApi.TrophiesAndAwards,
    hasSimul: Boolean,
    ratingChart: Option[String],
    nbs: UserInfo.NbGames,
    nbFollowers: Int,
    nbForumPosts: Int,
    ublog: Option[UblogPost.BlogPreview],
    nbStudies: Int,
    nbSimuls: Int,
    nbRelays: Int,
    teamIds: List[lila.team.TeamId],
    isStreamer: Boolean,
    isCoach: Boolean,
    insightVisible: Boolean
):
  export trophies.ranks
  export nbs.crosstable

object UserInfo:

  enum Angle(val key: String):
    case Activity                           extends Angle("activity")
    case Games(searchForm: Option[Form[?]]) extends Angle("games")
    case Other                              extends Angle("other")

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
    def apply(u: User)(using ctx: Context): Fu[Social] =
      ctx.userId.so {
        relationApi.fetchRelation(_, u.id).mon(_.user segment "relation")
      } zip
        ctx.me.soUse { _ ?=>
          fetchNotes(u).mon(_.user segment "notes")
        } zip
        ctx.isAuth.so {
          prefApi.followable(u.id).mon(_.user segment "followable")
        } zip
        ctx.userId.so { myId =>
          relationApi.fetchBlocks(u.id, myId).mon(_.user segment "blocks")
        } dmap { case (((relation, notes), followable), blocked) =>
          Social(relation, notes, followable, blocked)
        }

    def fetchNotes(u: User)(using Me) =
      noteApi.get(u, Granter(_.ModNote)) dmap {
        _.filter: n =>
          (!n.dox || Granter(_.Admin))
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
      (withCrosstable so ctx.me.filterNot(u.is(_)) so { me =>
        crosstableApi.withMatchup(me, u.id) dmap some
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
      simulApi: lila.simul.SimulApi,
      relayApi: lila.relay.RelayApi,
      ratingChartApi: lila.history.RatingChartApi,
      userApi: lila.api.UserApi,
      isHostingSimul: lila.round.IsSimulHost,
      streamerApi: lila.streamer.StreamerApi,
      teamApi: lila.team.TeamApi,
      teamCache: lila.team.Cached,
      coachApi: lila.coach.CoachApi,
      insightShare: lila.insight.Share
  )(using Executor):
    def apply(user: User, nbs: NbGames, withUblog: Boolean = true)(using ctx: Context): Fu[UserInfo] =
      ((ctx.noBlind && ctx.pref.showRatings) so ratingChartApi(user)).mon(_.user segment "ratingChart") zip
        relationApi.countFollowers(user.id).mon(_.user segment "nbFollowers") zip
        (!user.is(User.lichessId) && !user.isBot).so {
          postApi.nbByUser(user.id).mon(_.user segment "nbForumPosts")
        } zip
        (withUblog so ublogApi.userBlogPreviewFor(user, 3)) zip
        studyRepo.countByOwner(user.id).recoverDefault.mon(_.user segment "nbStudies") zip
        simulApi.countHostedByUser.get(user.id).mon(_.user segment "nbSimuls") zip
        relayApi.countOwnedByUser.get(user.id).mon(_.user segment "nbBroadcasts") zip
        userApi.getTrophiesAndAwards(user).mon(_.user segment "trophies") zip
        teamApi.joinedTeamIdsOfUserAsSeenBy(user).mon(_.user segment "teamIds") zip
        coachApi.isListedCoach(user).mon(_.user segment "coach") zip
        streamerApi.isActualStreamer(user).mon(_.user segment "streamer") zip
        (user.count.rated >= 10).so(insightShare.grant(user)) zip
        (nbs.playing > 0).so(isHostingSimul(user.id).mon(_.user segment "simul")) map {
          // format: off
          case ((((((((((((ratingChart, nbFollowers), nbForumPosts), ublog), nbStudies), nbSimuls), nbRelays), trophiesAndAwards), teamIds), isCoach), isStreamer), insightVisible), hasSimul) =>
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
              nbSimuls = nbSimuls,
              nbRelays = nbRelays,
              trophies = trophiesAndAwards,
              teamIds = teamIds,
              isStreamer = isStreamer,
              isCoach = isCoach,
              insightVisible = insightVisible
            )
        }

    def preloadTeams(info: UserInfo) = teamCache.nameCache.preloadMany(info.teamIds)
