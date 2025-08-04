package lila.api

import play.api.libs.json.{ Json, JsObject }
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.core.perf.UserWithPerfs
import lila.core.i18n.Translate
import lila.common.Json.given

final class MobileApi(
    userApi: UserApi,
    gameApi: GameApiV2,
    lobbyApi: LobbyApi,
    lightUserApi: lila.core.user.LightUserApi,
    gameProxy: lila.round.GameProxyRepo,
    unreadCount: lila.msg.MsgUnreadCount,
    teamCached: lila.team.Cached,
    tourFeaturing: lila.tournament.TournamentFeaturing,
    tourApiJson: lila.tournament.ApiJsonView,
    topRelay: Int => lila.relay.JsonView.Config ?=> Fu[JsObject],
    tv: lila.tv.Tv,
    liveStreamApi: lila.streamer.LiveStreamApi,
    picfitUrl: lila.core.misc.PicfitUrl
)(using Executor):

  private given (using trans: Translate): Lang = trans.lang

  def home(using me: Option[Me])(using RequestHeader, Translate): Fu[JsObject] =
    val accountFu = me.map(userApi.forMobileHome(using _))
    val recentGamesFu = me.map(gameApi.forMobileHome(using _))
    val ongoingGamesFu = me.map: u =>
      gameProxy.urgentGames(u).map(_.take(20).map(lobbyApi.nowPlaying))
    val tournamentsFu = me.map: u =>
      for
        perfs <- userApi.withPerfs(u.value)
        tours <- featuredTournaments(using perfs.some)
      yield tours
    val inboxFu = me.map(me => unreadCount.mobile(me.value))
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

  def watch: Fu[JsObject] =
    for
      relay <- topRelay(1)(using lila.relay.JsonView.Config(html = false))
      champs <- tv.getChampions
      tvChannels = champs.channels.mapKeys(_.key)
      streamers <- featuredStreamers
    yield Json.obj(
      "broadcast" -> relay,
      "tv" -> Json.toJson(tvChannels),
      "streamers" -> streamers
    )

  def featuredStreamers = for
    s <- liveStreamApi.all
    users <- lightUserApi.asyncManyFallback(s.streams.map(_.streamer.userId))
  yield s.streams
    .zip(users)
    .map: (stream, user) =>
      Json.toJsObject(user) ++
        lila.streamer.Stream.toJson(picfitUrl, stream)
