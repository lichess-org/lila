package lila.api

import play.api.libs.json.{ Json, JsObject }
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import scalalib.data.Preload

import lila.common.Json.given
import lila.core.i18n.Translate
import lila.core.user.KidMode
import lila.core.net.UserAgent
import lila.oauth.TokenScopes
import lila.core.perf.UserWithPerfs

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
    picfitUrl: lila.memo.PicfitUrl,
    isOnline: lila.core.socket.IsOnline
)(using Executor):

  private given (using trans: Translate): Lang = trans.lang

  def home(oauth: Option[TokenScopes])(using
      me: Option[Me],
      ua: UserAgent
  )(using RequestHeader, Translate, KidMode): Fu[JsObject] =
    val myUser = me.map(_.value)
    val takex3 = oauth.exists(_.takex3)
    for
      withPerfs <- myUser.traverse(userApi.withPerfs)
      urgentGames <- myUser.traverse(gameProxy.urgentGames)
      ongoingGames = urgentGames.map(_.value.take(20).map(lobbyApi.nowPlaying))
      tours <- takex3.not.option(tournamentsOf(withPerfs)).sequence
      account <- withPerfs.traverse(userApi.mobile(_, Preload(urgentGames)))
      recentGames <- myUser.traverse(gameApi.mobileRecent)
      inbox <- me.ifFalse(takex3).traverse(unreadCount.mobile)
      challenges <- me.traverse(challengeApi.allFor(_))
    yield Json
      .obj()
      .add("tournaments", tours)
      .add("account", account)
      .add("recentGames", recentGames)
      .add("ongoingGames", ongoingGames)
      .add("inbox", inbox)
      .add("challenges", challenges.map(challengeJson.all))

  def tournamentsOf(me: Option[UserWithPerfs])(using Translate): Fu[JsObject] =
    for
      teamIds <- me.so(teamCached.teamIdsList)
      tours <- tourFeaturing.homepage.get(teamIds)
      spotlight = lila.tournament.Spotlight.select(tours, 4)(using me)
      json <- spotlight.sequentially(tourApiJson.fullJson)
    yield Json.obj("featured" -> json)

  def tournaments(using me: Option[Me])(using Translate): Fu[JsObject] =
    for
      withPerfs <- me.so(userApi.withPerfs)
      tours <- tournamentsOf(withPerfs)
    yield tours

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
      withPerfs <- userApi.withPerfs(user)
      prof <- userApi.mobile(withPerfs, Preload.none)
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
