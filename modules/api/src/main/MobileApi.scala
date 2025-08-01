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

  def home(using me: Me)(using RequestHeader, Translate): Fu[JsObject] =
    val accountFu = userApi.forMobileHome
    val recentGamesFu = gameApi.forMobileHome
    val ongoingGamesFu = gameProxy
      .urgentGames(me)
      .map(_.take(20).map(lobbyApi.nowPlaying))
    val tournamentsFu = for
      perfs <- userApi.withPerfs(me.value) // TODO refetches the user
      tours <- featuredTournaments(using perfs.some)
    yield tours
    val inboxFu = unreadCount.mobile(me)
    (accountFu, recentGamesFu, ongoingGamesFu, tournamentsFu, inboxFu).mapN:
      (account, recentGames, ongoingGames, tournaments, inbox) =>
        Json.obj(
          "account" -> account,
          "recentGames" -> recentGames,
          "ongoingGames" -> ongoingGames,
          "tournaments" -> tournaments,
          "inbox" -> inbox
        )

  def featuredTournaments(using me: Option[UserWithPerfs])(using Translate): Fu[List[JsObject]] =
    for
      teamIds <- me.map(_.user.id).so(teamCached.teamIdsList)
      tours <- tourFeaturing.homepage.get(teamIds)
      spotlight = lila.tournament.Spotlight.select(tours, 4)
      json <- spotlight.sequentially(tourApiJson.fullJson)
    yield json
