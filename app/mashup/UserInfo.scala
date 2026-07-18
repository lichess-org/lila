package lila.app
package mashup

import alleycats.Zero
import play.api.data.Form

import lila.bookmark.BookmarkApi
import lila.core.data.SafeJsonStr
import lila.core.perf.UserWithPerfs
import lila.core.user.User
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
  )(using Executor):
    def apply(u: User)(using ctx: Context): Fu[Social] =
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

  object NbGames:
    given Zero[NbGames] = Zero(NbGames(none, 0, 0, 0))

  final class NbGamesApi(
      bookmarkApi: BookmarkApi,
      gameCached: lila.game.Cached,
      crosstableApi: lila.game.CrosstableApi
  )(using Executor):
    def apply(u: User, withCrosstable: Boolean)(using me: Option[Me]): Fu[NbGames] =
      (
        withCrosstable.so:
          me
            .filter(u.isnt(_))
            .traverse: me =>
              crosstableApi.withMatchup(me.userId, u.id).mon(lila.mon.user.segment("crosstable"))
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
    def fetch(user: User, nbs: NbGames, restricted: Boolean, withBlog: Boolean = true)(using
        ctx: Context
    ): Fu[UserInfo] =
      val full = !restricted
      def showRatings = full && ctx.noBlind && ctx.pref.showRatings
      (
        perfsRepo.withPerfs(user),
        userApi.getTrophiesAndAwards(user).mon(lila.mon.user.segment("trophies")),
        (nbs.playing > 0).so(simulApi.isSimulHost(user.id).mon(lila.mon.user.segment("simul"))),
        showRatings.so(ratingChartApi(user)).mon(lila.mon.user.segment("ratingChart")),
        (!user.is(UserId.lichess) && !user.isBot).so:
          postApi.nbByUser(user.id).mon(lila.mon.user.segment("nbForumPosts"))
        ,
        (withBlog && full).so(ublogApi.userBlogPreviewFor(user, 3)),
        full.so:
          studyRepo.countByOwner(user.id).recoverDefault.mon(lila.mon.user.segment("nbStudies"))
        ,
        full.so(simulApi.countHostedByUser.get(user.id).mon(lila.mon.user.segment("nbSimuls"))),
        full.so(relayApi.countOwnedByUser.get(user.id).mon(lila.mon.user.segment("nbBroadcasts"))),
        full.so(ctx.useMe(teamApi.joinedTeamIdsOfUserAsSeenBy(user).mon(lila.mon.user.segment("teamIds")))),
        streamerApi.isActualStreamer(user).mon(lila.mon.user.segment("streamer")),
        coachApi.isListedCoach(user).mon(lila.mon.user.segment("coach")),
        fideIdOf(user.light),
        fuccess(Granter.opt(_.SeeInsight)) >>| (user.count.rated >= 50).so(insightShare.grant(user))
      ).mapN(UserInfo(nbs, _, _, _, _, _, _, _, _, _, _, _, _, _, _))

    def preloadTeams(info: UserInfo) = teamCache.lightCache.preloadMany(info.teamIds)
