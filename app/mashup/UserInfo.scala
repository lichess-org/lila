package lila.app
package mashup

import play.api.data.Form

import lila.bookmark.BookmarkApi
import lila.core.data.SafeJsonStr
import lila.core.perf.UserWithPerfs
import lila.core.user.User
import lila.forum.ForumPostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.ublog.{ UblogApi, UblogPost }

case class UserInfo(
    nbs: UserInfo.NbGames,
    user: UserWithPerfs,
    trophies: lila.api.UserApi.TrophiesAndAwards,
    hasSimul: Boolean,
    ratingChart: Option[SafeJsonStr],
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
      given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.parasitic
      (
        ctx.userId.so(relationApi.fetchRelation(_, u.id).mon(_.user.segment("relation"))),
        ctx.me.soUse(_ ?=> noteApi.getForMyPermissions(u).mon(_.user.segment("notes"))),
        ctx.isAuth.so(prefApi.followable(u.id).mon(_.user.segment("followable"))),
        ctx.userId.so(myId => relationApi.fetchBlocks(u.id, myId).mon(_.user.segment("blocks")))
      ).mapN(Social.apply)

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
    def apply(u: User, withCrosstable: Boolean)(using me: Option[Me]): Fu[NbGames] =
      given scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.parasitic
      (
        withCrosstable.so(
          me
            .filter(u.isnt(_))
            .soFu(me => crosstableApi.withMatchup(me.userId, u.id).mon(_.user.segment("crosstable")))
        ),
        gameCached.nbPlaying(u.id).mon(_.user.segment("nbPlaying")),
        gameCached.nbImportedBy(u.id).mon(_.user.segment("nbImported")),
        bookmarkApi.countByUser(u).mon(_.user.segment("nbBookmarks"))
      ).mapN(NbGames.apply)

  final class UserInfoApi(
      postApi: ForumPostApi,
      ublogApi: UblogApi,
      perfsRepo: lila.user.UserPerfsRepo,
      studyRepo: lila.study.StudyRepo,
      simulApi: lila.simul.SimulApi,
      relayApi: lila.relay.RelayApi,
      ratingChartApi: lila.history.RatingChartApi,
      userApi: lila.api.UserApi,
      streamerApi: lila.streamer.StreamerApi,
      teamApi: lila.team.TeamApi,
      teamCache: lila.team.Cached,
      coachApi: lila.coach.CoachApi,
      insightShare: lila.insight.Share
  )(using Executor):
    def apply(user: User, nbs: NbGames, withUblog: Boolean = true)(using ctx: Context): Fu[UserInfo] =
      (
        perfsRepo.withPerfs(user),
        userApi.getTrophiesAndAwards(user).mon(_.user.segment("trophies")),
        (nbs.playing > 0).so(simulApi.isSimulHost(user.id).mon(_.user.segment("simul"))),
        ((ctx.noBlind && ctx.pref.showRatings).so(ratingChartApi(user))).mon(_.user.segment("ratingChart")),
        (!user.is(UserId.lichess) && !user.isBot).so {
          postApi.nbByUser(user.id).mon(_.user.segment("nbForumPosts"))
        },
        withUblog.so(ublogApi.userBlogPreviewFor(user, 3)),
        studyRepo.countByOwner(user.id).recoverDefault.mon(_.user.segment("nbStudies")),
        simulApi.countHostedByUser.get(user.id).mon(_.user.segment("nbSimuls")),
        relayApi.countOwnedByUser.get(user.id).mon(_.user.segment("nbBroadcasts")),
        teamApi.joinedTeamIdsOfUserAsSeenBy(user).mon(_.user.segment("teamIds")),
        streamerApi.isActualStreamer(user).mon(_.user.segment("streamer")),
        coachApi.isListedCoach(user).mon(_.user.segment("coach")),
        (user.count.rated >= 10).so(insightShare.grant(user)(using ctx.me))
      ).mapN(UserInfo(nbs, _, _, _, _, _, _, _, _, _, _, _, _, _))

    def preloadTeams(info: UserInfo) = teamCache.lightCache.preloadMany(info.teamIds)
