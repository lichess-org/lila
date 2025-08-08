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
    activityRead: lila.activity.ActivityReadApi,
    activityJsonView: lila.activity.JsonView,
    picfitUrl: lila.core.misc.PicfitUrl,
    isOnline: lila.core.socket.IsOnline
)(using Executor):

  private given (using trans: Translate): Lang = trans.lang

  def home(using me: Option[Me])(using RequestHeader, Translate): Fu[JsObject] =
    val tournamentsFu = for
      perfs <- me.map(me => userApi.withPerfs(me.value)).sequence
      tours <- featuredTournaments(using perfs)
    yield tours
    val accountFu = me.map(u => userApi.mobile(u.value))
    val recentGamesFu = me.map(u => gameApi.mobileRecent(u.value))
    val ongoingGamesFu = me.map: u =>
      gameProxy.urgentGames(u).map(_.take(20).map(lobbyApi.nowPlaying))
    val inboxFu = me.map(unreadCount.mobile)
    for
      tournaments <- tournamentsFu
      account <- accountFu.sequence
      recentGames <- recentGamesFu.sequence
      ongoingGames <- ongoingGamesFu.sequence
      inbox <- inboxFu.sequence
    yield Json
      .obj("tournaments" -> tournaments)
      .add("account", account)
      .add("recentGames", recentGames)
      .add("ongoingGames", ongoingGames)
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

  def profile(user: User)(using me: Option[Me])(using Lang): Fu[JsObject] =
    for
      prof <- userApi.mobile(user)
      activities <- activityRead.recentAndPreload(user)
      activity <- activities.sequentially(activityJsonView(_, user))
      games <- gameApi.mobileRecent(user)
      status <- me.forall(_.isnt(user)).soFu(userStatus(user))
      crosstable <- me.filter(_.isnt(user)).map(gameApi.crosstableWith(user)).sequence
    yield Json
      .obj("profile" -> prof, "activity" -> activity, "games" -> games)
      .add("status", status)
      .add("crosstable", crosstable)

  private def userStatus(user: User)(using Option[Me]): Fu[JsObject] =
    for playing <- gameApi.mobileCurrent(user)
    yield Json
      .obj()
      .add("online", isOnline.exec(user.id))
      .add("playing", playing)
      .add("streaming", liveStreamApi.userIds(user.id))
