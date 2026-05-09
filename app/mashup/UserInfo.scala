package lila.app
package mashup

import play.api.data.Form

import lila.bookmark.BookmarkApi
import lila.core.data.SafeJsonStr
import lila.core.perf.UserWithPerfs
import lila.core.user.User
import lila.core.security.IsProxy
import lila.core.perm.Granter
import lila.forum.ForumPostApi
import lila.game.Crosstable
import lila.relation.RelationApi
import lila.ublog.{ UblogApi, UblogPost }
import lila.mon.extensions.*

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
    publicFideId: Option[chess.FideId],
    insightVisible: Boolean
):
  export trophies.ranks
  export nbs.crosstable

object UserInfo:

  enum Angle(val key: String):
    case Activity extends Angle("activity")
    case Games(searchForm: Option[Form[?]]) extends Angle("games")
    case Other extends Angle("other")

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
        ctx.userId.so(relationApi.fetchRelation(_, u.id).mon(lila.mon.user.segment("relation"))),
        ctx.useMe(noteApi.getForMyPermissions(u).mon(lila.mon.user.segment("notes"))),
        ctx.isAuth.so(prefApi.followable(u.id).mon(lila.mon.user.segment("followable"))),
        ctx.userId.so(myId => relationApi.fetchBlocks(u.id, myId).mon(lila.mon.user.segment("blocks")))
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
        withCrosstable.so:
          me
            .filter(u.isnt(_))
            .traverse(me =>
              crosstableApi.withMatchup(me.userId, u.id).mon(lila.mon.user.segment("crosstable"))
            )
        ,
        gameCached.nbPlaying(u.id).mon(lila.mon.user.segment("nbPlaying")),
        gameCached.nbImportedBy(u.id).mon(lila.mon.user.segment("nbImported")),
        bookmarkApi.countByUser(u).mon(lila.mon.user.segment("nbBookmarks"))
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
      teamCache: lila.team.TeamCached,
      coachApi: lila.coach.CoachApi,
      fideIdOf: lila.core.user.PublicFideIdOf,
      insightShare: lila.insight.Share
  )(using Executor):
    def fetch(user: User, nbs: NbGames, withUblog: Boolean = true)(using
        ctx: Context,
        proxy: IsProxy
    ): Fu[UserInfo] =
      def isAuthOrNotProxied = ctx.isAuth || (!proxy.isFloodish && !proxy.isCrawler)
      def showRatings = ctx.noBlind && ctx.pref.showRatings && isAuthOrNotProxied
      (
        perfsRepo.withPerfs(user),
        userApi.getTrophiesAndAwards(user).mon(lila.mon.user.segment("trophies")),
        (nbs.playing > 0).so(simulApi.isSimulHost(user.id).mon(lila.mon.user.segment("simul"))),
        showRatings.so(ratingChartApi(user)).mon(lila.mon.user.segment("ratingChart")),
        (!user.is(UserId.lichess) && !user.isBot).so:
          postApi.nbByUser(user.id).mon(lila.mon.user.segment("nbForumPosts"))
        ,
        withUblog.so(ublogApi.userBlogPreviewFor(user, 3)),
        studyRepo.countByOwner(user.id).recoverDefault.mon(lila.mon.user.segment("nbStudies")),
        simulApi.countHostedByUser.get(user.id).mon(lila.mon.user.segment("nbSimuls")),
        relayApi.countOwnedByUser.get(user.id).mon(lila.mon.user.segment("nbBroadcasts")),
        ctx.useMe(teamApi.joinedTeamIdsOfUserAsSeenBy(user).mon(lila.mon.user.segment("teamIds"))),
        streamerApi.isActualStreamer(user).mon(lila.mon.user.segment("streamer")),
        coachApi.isListedCoach(user).mon(lila.mon.user.segment("coach")),
        fideIdOf(user.light),
        fuccess(Granter.opt(_.SeeInsight)) >>| (user.count.rated >= 50).so(insightShare.grant(user))
      ).mapN(UserInfo(nbs, _, _, _, _, _, _, _, _, _, _, _, _, _, _))

    def preloadTeams(info: UserInfo) = teamCache.lightCache.preloadMany(info.teamIds)
