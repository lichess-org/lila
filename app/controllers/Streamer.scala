package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.streamer.{ Streamer as StreamerModel, StreamerForm }

final class Streamer(env: Env, apiC: => Api) extends LilaController(env):

  import env.streamer.api
  import env.mod.logApi

  def index(page: Int) = Open: ctx ?=>
    NoBot:
      ctx.kid.no.so:
        val requests = getBool("requests") && isGrantedOpt(_.Streamers)
        for
          liveStreams <- env.streamer.liveStreamApi.all
          live <- api.withUsers(liveStreams)
          pager <- env.streamer.pager.get(page, liveStreams, requests)
          page <- renderPage(views.streamer.index(live, pager, requests))
        yield Ok(page)

  def featured = Anon: ctx ?=>
    env.streamer.liveStreamApi.all.map: streams =>
      val max = env.streamer.homepageMaxSetting.get()
      val featured = streams.homepage(max, ctx.acceptLanguages).withTitles(env.user.lightUserApi)
      JsonOk:
        featured.live.streams.map: s =>
          Json.obj(
            "url" -> routes.Streamer.redirect(s.streamer.userId).absoluteURL(),
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
          sws <- env.streamer.liveStreamApi.of(s)
          activity <- env.activity.read.recentAndPreload(sws.user)
          perfs <- env.user.perfsRepo.perfsOf(sws.user)
          page <- renderPage(views.streamer.show(sws, perfs, activity))
        yield Ok(page)

  def redirect(username: UserStr) = Open:
    Found(api.forSubscriber(username)): s =>
      WithVisibleStreamer(s):
        env.streamer.liveStreamApi.of(s).map { sws =>
          Redirect(sws.redirectToLiveUrl | routes.Streamer.show(username).url)
        }

  def create = AuthBody { _ ?=> me ?=>
    ctx.kid.no.so:
      NoLameOrBot:
        api
          .find(me)
          .flatMap:
            case None => api.create(me).inject(Redirect(routes.Streamer.edit))
            case _ => Redirect(routes.Streamer.edit)
  }

  private def modData(streamer: StreamerModel)(using ctx: Context) =
    (isGrantedOpt(_.ModLog) && ctx.isnt(streamer)).optionFu:
      logApi
        .userHistory(streamer.userId)
        .zip(env.user.noteApi.toUserForMod(streamer.userId))
        .zip(env.streamer.api.sameChannels(streamer))

  def edit = Auth { ctx ?=> _ ?=>
    AsStreamer: s =>
      for
        _ <- env.msg.twoFactorReminder(s.user.id)
        sws <- env.streamer.liveStreamApi.of(s)
        forMod <- modData(s.streamer)
        page <- renderPage(views.streamer.edit(sws, StreamerForm.userForm(sws.streamer), forMod))
      yield Ok(page).noCache
  }

  def editApply = AuthBody { _ ?=> me ?=>
    AsStreamer: s =>
      env.streamer.liveStreamApi.of(s).flatMap { sws =>
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

  def pictureApply = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    AsStreamer: s =>
      ctx.body.body.file("picture") match
        case Some(pic) =>
          limit.imageUpload(ctx.ip, rateLimited):
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
        .streamerOnlineCheck(uid, rateLimited)(env.streamer.api.forceCheck(uid))
        .inject(
          Redirect(routes.Streamer.show(uid).url)
            .flashSuccess(s"Please wait one minute while we check, then reload the page.")
        )
    else Unauthorized
  }

  def onYouTubeVideo = AnonBodyOf(parse.tolerantXml): body =>
    env.streamer.ytApi.onVideoXml(body)
    NoContent

  def youTubePubSubChallenge = Anon:
    get("hub.challenge").fold(BadRequest): challenge =>
      val days = get("hub.lease_seconds").map(s => f" for ${s.toFloat / (60 * 60 * 24)}%.1f days")
      val channelId = get("hub.topic").map(t => s" on ${t.split("=").last}")
      lila
        .log("streamer")
        .debug(s"WebSub: CONFIRMED ${~get("hub.mode")}${~days}${~channelId}")
      Ok(challenge)

  private def AsStreamer(f: StreamerModel.WithContext => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me.foldUse(notFound): me ?=>
      if StreamerModel.canApply(me) || isGranted(_.Streamers) then
        api
          .find(getUserStr("u").ifTrue(isGranted(_.Streamers)).map(_.id) | me.userId)
          .flatMap:
            _.fold(Ok.page(views.streamer.create))(f)
      else
        Ok.page:
          views.site.message("Too soon"):
            scalatags.Text.all.raw("You are not yet allowed to create a streamer profile.")

  private def WithVisibleStreamer(s: StreamerModel.WithContext)(f: Fu[Result])(using ctx: Context) =
    ctx.kid.no.so:
      if s.streamer.isListed || ctx.me.exists(_.is(s.streamer)) || isGrantedOpt(_.Admin)
      then f
      else notFound
