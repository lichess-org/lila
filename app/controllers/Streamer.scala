package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.core.perm.Granter
import lila.streamer.{ Streamer as StreamerModel, StreamerForm, Platform }

final class Streamer(env: Env, apiC: => Api) extends LilaController(env):

  import env.streamer.api
  import env.mod.logApi

  def index(page: Int) = Open: ctx ?=>
    NoBot:
      ctx.kid.no.so:
        val requests = getBool("requests") && isGrantedOpt(_.Streamers)
        for
          liveStreams <- env.streamer.liveApi.all
          live <- api.withUsers(liveStreams)
          pager <- env.streamer.pager.get(page, liveStreams, requests)
          page <- renderPage(views.streamer.index(live, pager, requests))
        yield Ok(page)

  def featured = Anon: ctx ?=>
    env.streamer.liveApi.all.map: streams =>
      val max = env.streamer.homepageMaxSetting.get()
      val featured = streams.homepage(max, ctx.acceptLanguages).withTitles(env.user.lightUserApi)
      JsonOk:
        featured.live.streams.map: s =>
          Json.obj(
            "url" -> routeUrl(routes.Streamer.redirect(s.streamer.userId)),
            "status" -> s.status,
            "user" -> Json
              .obj(
                "id" -> s.streamer.userId,
                "name" -> s.streamer.name
              )
              .add("title" -> featured.titles.get(s.streamer.userId))
          )

  def live = apiC.ApiRequest:
    env.api.mobile.featuredStreamers.map(apiC.toApiResult)

  def show(username: UserStr) = Open:
    Found(api.forSubscriber(username)): s =>
      WithVisibleStreamer(s):
        for
          sws <- env.streamer.liveApi.of(s)
          activity <- env.activity.read.recentAndPreload(sws.user)
          perfs <- env.user.perfsRepo.perfsOf(sws.user)
          page <- renderPage(views.streamer.show(sws, perfs, activity))
        yield Ok(page)

  def redirect(username: UserStr) = Open:
    Found(api.forSubscriber(username)): s =>
      WithVisibleStreamer(s):
        env.streamer.liveApi.of(s).map { sws =>
          Redirect(sws.redirectToLiveUrl | routes.Streamer.show(username).url)
        }

  def create = AuthBody { _ ?=> me ?=>
    ctx.kid.no.so:
      NoLameOrBot:
        env.streamer.api
          .find(me)
          .flatMap:
            case None => env.streamer.repo.create(me).inject(Redirect(routes.Streamer.edit))
            case _ => Redirect(routes.Streamer.edit)
  }

  private def modData(streamer: StreamerModel)(using ctx: Context) =
    (isGrantedOpt(_.ModLog) && ctx.isnt(streamer)).optionFu:
      logApi
        .userHistory(streamer.userId)
        .zip(env.user.noteApi.toUserForMod(streamer.userId))

  def edit = Auth { ctx ?=> _ ?=>
    AsStreamer: s =>
      for
        _ <- env.msg.systemMsg.twoFactorReminder(s.user.id)
        sws <- env.streamer.liveApi.of(s)
        forMod <- modData(s.streamer)
        page <- renderPage(views.streamer.edit(sws, StreamerForm.userForm(sws.streamer), forMod))
      yield Ok(page).noCache
  }

  def editApply = AuthBody { _ ?=> me ?=>
    AsStreamer: s =>
      env.streamer.liveApi.of(s).flatMap { sws =>
        bindForm(StreamerForm.userForm(sws.streamer))(
          error =>
            modData(s.streamer).flatMap: forMod =>
              BadRequest.page(views.streamer.edit(sws, error, forMod)),
          data =>
            api
              .update(sws.streamer, data, isGranted(_.Streamers))
              .flatMap:
                case Some(change) =>
                  if change.decline then logApi.streamerDecline(s.user.id, change.reason)
                  change.list.foreach { logApi.streamerList(s.user.id, _) }
                  change.tier.foreach { logApi.streamerTier(s.user.id, _) }
                  if data.approval.quick.isDefined
                  then
                    env.streamer.pager.nextRequestId.map: nextId =>
                      Redirect:
                        nextId.fold(s"${routes.Streamer.index()}?requests=1"): id =>
                          s"${routes.Streamer.edit.url}?u=$id"
                  else
                    val next = sws.streamer.isnt(me).so(s"?u=${sws.user.id}")
                    Redirect(s"${routes.Streamer.edit.url}$next")
                case _ =>
                  Redirect(routes.Streamer.edit)
        )
      }
  }

  def pictureApply = AuthBody(lila.web.HashedMultiPart(parse)) { ctx ?=> me ?=>
    AsStreamer: s =>
      ctx.body.body.file("picture") match
        case Some(pic) =>
          limit.imageUpload(rateLimited):
            api
              .uploadPicture(s.streamer, pic, me)
              .recoverWith { case _: Exception =>
                Redirect(routes.Streamer.edit).flashFailure
              }
              .inject(Ok)
        case None => Redirect(routes.Streamer.edit).flashFailure
  }

  def subscribe(streamer: UserStr, set: Boolean) = AuthBody { _ ?=> me ?=>
    if set
    then env.relation.subs.subscribe(me, streamer.id)
    else env.relation.subs.unsubscribe(me, streamer.id)
    Ok
  }

  def checkOnline(streamer: UserStr) = Auth { _ ?=> me ?=>
    val uid = streamer.id
    val isMod = isGranted(_.ModLog)
    if ctx.is(uid) || isMod then
      limit
        .streamerOnlineCheck(uid, rateLimited)(api.forceCheck(uid))
        .inject(
          Redirect(routes.Streamer.show(uid).url)
            .flashSuccess(s"Please wait one minute while we check, then reload the page.")
        )
    else Unauthorized
  }

  def onTwitchEventSub = AnonBodyOf(parse.tolerantText): body =>
    env.streamer.twitchApi
      .onMessage(body, req.headers)
      .map(_.fold(NoContent)(Ok(_)))

  def onYoutubeVideo = AnonBodyOf(parse.tolerantXml): body =>
    env.streamer.ytApi.onVideoXml(body)
    NoContent

  def youtubePubSubChallenge = Anon:
    get("hub.challenge").fold(BadRequest): challenge =>
      val days = get("hub.lease_seconds").map(s => f" for ${s.toFloat / (60 * 60 * 24)}%.1f days")
      val channelId = get("hub.topic").map(t => s" on ${t.split("=").last}")
      lila
        .log("streamer")
        .debug(s"WebSub: CONFIRMED ${~get("hub.mode")}${~days}${~channelId}")
      Ok(challenge)

  private def WithVisibleStreamer(s: StreamerModel.WithContext)(f: => Fu[Result])(using ctx: Context) =
    ctx.kid.no.so:
      if s.streamer.isListed || ctx.is(s.streamer) || isGrantedOpt(_.Admin)
      then f
      else notFound

  private def AsStreamer(f: StreamerModel.WithContext => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me.foldUse(notFound): me ?=>
      if StreamerModel.canApply(me) || isGranted(_.Streamers) then
        env.streamer.api
          .find(getUserStr("u").ifTrue(isGranted(_.Streamers)).map(_.id) | me.userId)
          .flatMap:
            _.fold(Ok.page(views.streamer.create))(f)
      else
        Ok.page:
          views.site.message("Too soon"):
            scalatags.Text.all.raw("You are not yet allowed to create a streamer profile.")

  private def myStreamer(using me: Me) = env.streamer.repo.byId(me.userId.into(StreamerModel.Id))

  private def oauth = env.streamer.oauth

  private def oauthMakeCookie(platform: Platform, value: String) =
    env.security.lilaCookie.cookie(oauth.cookie.name(platform), value)

  def oauthUnlink(platformStr: String, user: Option[UserStr]) = Auth { _ ?=> me ?=>
    val targetUserId = Granter(_.Streamers).so(user.map(_.id)).getOrElse(me.userId)
    Found(env.streamer.repo.byId(targetUserId.into(StreamerModel.Id))): streamer =>
      lila.streamer
        .platform(platformStr)
        .fold(notFound): platform =>
          api.oauthUnlink(streamer, platform).inject(Ok)
  }

  def oauthLinkTwitch = Auth { _ ?=> _ ?=>
    Found(myStreamer): _ =>
      val state = oauth.makeState()
      val redirectUri = routeUrl(routes.Streamer.oauthTwitchRedirect)
      Redirect(oauth.authorizeUrl.twitch(redirectUri, state, getBool("force_verify")))
        .withCookies(oauthMakeCookie("twitch", state))
  }

  def oauthTwitchRedirect = Auth { _ ?=> _ ?=>
    (get("code"), get("state"), oauth.cookie.get("twitch")) match
      case (Some(code), Some(state), Some(expected)) if expected == state =>
        Found(myStreamer): streamer =>
          val redirectUri = routeUrl(routes.Streamer.oauthTwitchRedirect)
          for
            twitchUser <- oauth.twitchUser(code, redirectUri)
            result <- env.streamer.repo.oauth.linkTwitch(streamer, twitchUser)
          yield Ok
            .snip(views.streamer.oauth("twitch", redirectUri, Left(result)))
            .discardingCookies(oauth.cookie.unset("twitch"))
      case _ => fuccess(BadRequest)
  }

  def oauthLinkYoutube = Auth { _ ?=> _ ?=>
    Found(myStreamer): _ =>
      val state = oauth.makeState()
      val redirectUri = routeUrl(routes.Streamer.oauthYoutubeRedirect)
      val url = oauth.authorizeUrl.youtube(redirectUri, state, getBool("force_verify"))
      Redirect(url).withCookies(oauthMakeCookie("youtube", state))
  }

  def oauthYoutubeRedirect = Auth { _ ?=> me ?=>
    (get("code"), get("state"), oauth.cookie.get("youtube")) match
      case (Some(code), Some(state), Some(expected)) if expected == state =>
        Found(myStreamer): streamer =>
          val redirectUri = routeUrl(routes.Streamer.oauthYoutubeRedirect)
          for
            idsMap <- oauth.youtubeChannels(code, redirectUri)
            result <- idsMap.keys.toList match
              case id :: Nil => env.streamer.repo.oauth.linkYoutube(streamer, id).dmap(Left(_))
              case _ =>
                oauth.youtubeChannelCache.put(state, idsMap)
                fuccess(Right(idsMap))
            page = Ok.snip(views.streamer.oauth("youtube", redirectUri, result))
          // oauth won't pick the channel, if there's more than one, leave the cookie to do that securely
          yield if result.isLeft then page.discardingCookies(oauth.cookie.unset("youtube")) else page
      case _ => fuccess(BadRequest)
  }

  def oauthYoutubeChannel(channelId: String) = AuthBody { _ ?=> me ?=>
    Found(myStreamer): streamer =>
      oauth.youtubeChannelCache
        .find(channelId)
        .so: _ =>
          env.streamer.repo.oauth.linkYoutube(streamer, channelId).map(Ok(_))
        .map(_.discardingCookies(oauth.cookie.unset("youtube")))
  }
