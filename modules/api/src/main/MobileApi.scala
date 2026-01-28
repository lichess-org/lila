package lila.api

import play.api.libs.json.{ Json, JsObject }
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.common.Json.given
import lila.core.i18n.Translate
import lila.core.user.KidMode
import lila.core.net.UserAgent

final class MobileApi(
    userApi: UserApi,
    gameApi: GameApiV2,
    lobbyApi: LobbyApi,
    lightUserApi: lila.core.user.LightUserApi,
    gameProxy: lila.round.GameProxyRepo,
    unreadCount: lila.msg.MsgUnreadCount,
    teamCached: lila.team.TeamCached,
    tourFeaturing: lila.tournament.TournamentFeaturing,
    tourApiJson: lila.tournament.ApiJsonView,
    relayHome: lila.relay.RelayHomeApi,
    tv: lila.tv.Tv,
    liveStreamApi: lila.streamer.LiveApi,
    activityRead: lila.activity.ActivityReadApi,
    activityJsonView: lila.activity.JsonView,
    challengeApi: lila.challenge.ChallengeApi,
    challengeJson: lila.challenge.JsonView,
    webMobile: lila.web.Mobile,
    picfitUrl: lila.memo.PicfitUrl,
    isOnline: lila.core.socket.IsOnline
)(using Executor):

  private given (using trans: Translate): Lang = trans.lang

  def home(using me: Option[Me], ua: UserAgent)(using RequestHeader, Translate, KidMode): Fu[JsObject] =
    val myUser = me.map(_.value)
    for
      tours <- tournaments
      account <- myUser.traverse(userApi.mobile)
      recentGames <- myUser.traverse(gameApi.mobileRecent)
      ongoingGames <- myUser.traverse: u =>
        gameProxy.urgentGames(u).map(_.take(20).map(lobbyApi.nowPlaying))
      inbox <- me.traverse(unreadCount.mobile)
      challenges <- me.traverse(challengeApi.allFor(_))
    yield Json
      .obj("tournaments" -> tours, "version" -> webMobile.json)
      .add("account", account)
      .add("recentGames", recentGames)
      .add("ongoingGames", ongoingGames)
      .add("inbox", inbox)
      .add("challenges", challenges.map(challengeJson.all))

  def tournaments(using me: Option[Me])(using Translate): Fu[JsObject] =
    for
      perfs <- me.so(userApi.withPerfs)
      teamIds <- me.so(teamCached.teamIdsList)
      tours <- tourFeaturing.homepage.get(teamIds)
      spotlight = lila.tournament.Spotlight.select(tours, 4)(using perfs)
      json <- spotlight.sequentially(tourApiJson.fullJson)
    yield Json.obj("featured" -> json)

  def watch(using Translate): Fu[JsObject] =
    for
      relay <- relayHome.getJson(1)(using lila.relay.RelayJsonView.Config(html = false))
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
        lila.streamer.Stream.toLichessJson(picfitUrl, stream)

  def profile(user: User)(using me: Option[Me])(using Lang): Fu[JsObject] =
    for
      prof <- userApi.mobile(user)
      activities <- activityRead.recentAndPreload(user)
      activity <- activities.sequentially(activityJsonView(_, user))
      games <- gameApi.mobileRecent(user)
      status <- me.forall(_.isnt(user)).optionFu(userStatus(user))
      crosstable <- me.filter(_.isnt(user)).traverse(gameApi.crosstableWith(user))
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
