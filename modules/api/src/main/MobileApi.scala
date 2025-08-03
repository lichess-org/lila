package lila.api

import play.api.libs.json.{ Json, JsObject }
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.core.perf.UserWithPerfs
import lila.core.i18n.Translate

final class MobileApi(
    userApi: UserApi,
    gameApi: GameApiV2,
    lobbyApi: LobbyApi,
    gameProxy: lila.round.GameProxyRepo,
    unreadCount: lila.msg.MsgUnreadCount,
    teamCached: lila.team.Cached,
    tourFeaturing: lila.tournament.TournamentFeaturing,
    tourApiJson: lila.tournament.ApiJsonView
)(using Executor):

  private given (using trans: Translate): Lang = trans.lang

  def home(
      getAccount: Boolean,
      getRecentGames: Boolean,
      getOngoingGames: Boolean,
      getTournaments: Boolean,
      getInbox: Boolean
  )(using me: Me)(using RequestHeader, Translate): Fu[JsObject] =
    val accountFu = getAccount.option(userApi.forMobileHome)
    val recentGamesFu = getRecentGames.option(gameApi.forMobileHome)
    val ongoingGamesFu = getOngoingGames.option:
      gameProxy.urgentGames(me).map(_.take(20).map(lobbyApi.nowPlaying))
    val tournamentsFu = getTournaments.option:
      for
        perfs <- userApi.withPerfs(me.value)
        tours <- featuredTournaments(using perfs.some)
      yield tours
    val inboxFu = getInbox.option(unreadCount.mobile(me))
    for
      account <- accountFu.sequence
      recentGames <- recentGamesFu.sequence
      ongoingGames <- ongoingGamesFu.sequence
      tournaments <- tournamentsFu.sequence
      inbox <- inboxFu.sequence
    yield Json
      .obj()
      .add("account", account)
      .add("recentGames", recentGames)
      .add("ongoingGames", ongoingGames)
      .add("tournaments", tournaments)
      .add("inbox", inbox)

  def featuredTournaments(using me: Option[UserWithPerfs])(using Translate): Fu[List[JsObject]] =
    for
      teamIds <- me.map(_.user.id).so(teamCached.teamIdsList)
      tours <- tourFeaturing.homepage.get(teamIds)
      spotlight = lila.tournament.Spotlight.select(tours, 4)
      json <- spotlight.sequentially(tourApiJson.fullJson)
    yield json
