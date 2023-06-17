package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.api.WebContext
import lila.app.{ given, * }
import lila.streamer.{ Streamer as StreamerModel, StreamerForm }
import lila.common.Json.given

final class Streamer(env: Env, apiC: => Api) extends LilaController(env):

  import env.streamer.api
  import env.mod.logApi

  def index(page: Int) = Open:
    NoBot:
      ctx.noKid.so:
        pageHit
        val requests = getBool("requests") && isGranted(_.Streamers)
        for
          liveStreams <- env.streamer.liveStreamApi.all
          live        <- api.withUsers(liveStreams, ctx.me.map(_.id))
          pager       <- env.streamer.pager.get(page, liveStreams, ctx.me.map(_.id), requests)
        yield Ok(html.streamer.index(live, pager, requests))

  def featured = Anon:
    env.streamer.liveStreamApi.all.map: streams =>
      val max      = env.streamer.homepageMaxSetting.get()
      val featured = streams.homepage(max, req, none) withTitles env.user.lightUserApi
      JsonOk:
        featured.live.streams.map: s =>
          Json.obj(
            "url"    -> routes.Streamer.redirect(s.streamer.id.value).absoluteURL(),
            "status" -> s.status,
            "user" -> Json
              .obj(
                "id"   -> s.streamer.userId,
                "name" -> s.streamer.name
              )
              .add("title" -> featured.titles.get(s.streamer.userId))
          )

  def live = apiC.ApiRequest:
    for
      s     <- env.streamer.liveStreamApi.all
      users <- env.user.lightUserApi asyncManyFallback s.streams.map(_.streamer.userId)
    yield apiC.toApiResult:
      (s.streams zip users).map: (stream, user) =>
        lila.common.LightUser.lightUserWrites.writes(user) ++
          lila.streamer.Stream.toJson(env.memo.picfitUrl, stream)

  def show(username: UserStr) = Open:
    OptionFuResult(api.forSubscriber(username, ctx.me)): s =>
      WithVisibleStreamer(s):
        for
          sws      <- env.streamer.liveStreamApi of s
          activity <- env.activity.read.recentAndPreload(sws.user)
        yield Ok(html.streamer.show(sws, activity))

  def redirect(username: UserStr) = Open:
    OptionFuResult(api.forSubscriber(username, ctx.me)): s =>
      WithVisibleStreamer(s):
        env.streamer.liveStreamApi of s map { sws =>
          Redirect(sws.redirectToLiveUrl | routes.Streamer.show(username.value).url)
        }

  def create = AuthBody { _ ?=> me =>
    ctx.noKid.so:
      NoLameOrBot:
        api find me flatMap {
          case None => api.create(me) inject Redirect(routes.Streamer.edit)
          case _    => Redirect(routes.Streamer.edit)
        }
  }

  private def modData(streamer: StreamerModel)(using WebContext) =
    isGranted(_.ModLog).so:
      logApi.userHistory(streamer.userId) zip
        env.user.noteApi.byUserForMod(streamer.userId) zip
        env.streamer.api.sameChannels(streamer) map some

  def edit = Auth { ctx ?=> _ =>
    AsStreamer { s =>
      env.msg.twoFactorReminder(s.user.id) >>
        env.streamer.liveStreamApi.of(s).flatMap { sws =>
          modData(s.streamer) map { forMod =>
            Ok(html.streamer.edit(sws, StreamerForm userForm sws.streamer, forMod)).noCache
          }
        }
    }
  }

  def editApply = AuthBody { _ ?=> me =>
    AsStreamer: s =>
      env.streamer.liveStreamApi of s flatMap { sws =>
        StreamerForm
          .userForm(sws.streamer)
          .bindFromRequest()
          .fold(
            error =>
              modData(s.streamer) map { forMod =>
                BadRequest(html.streamer.edit(sws, error, forMod))
              },
            data =>
              api.update(sws.streamer, data, isGranted(_.Streamers)) flatMap { change =>
                if (change.decline) logApi.streamerDecline(lila.report.Mod(me), s.user.id)
                change.list foreach { logApi.streamerList(lila.report.Mod(me), s.user.id, _) }
                change.tier foreach { logApi.streamerTier(lila.report.Mod(me), s.user.id, _) }
                if (data.approval.flatMap(_.quick).isDefined)
                  env.streamer.pager.nextRequestId map { nextId =>
                    Redirect:
                      nextId.fold(s"${routes.Streamer.index()}?requests=1") { id =>
                        s"${routes.Streamer.edit.url}?u=$id"
                      }
                  }
                else
                  val next = if (sws.streamer is me) "" else s"?u=${sws.user.id}"
                  Redirect(s"${routes.Streamer.edit.url}$next")
              }
          )
      }
  }

  def approvalRequest = AuthBody { _ ?=> me =>
    NoBot:
      api.approval.request(me) inject Redirect(routes.Streamer.edit)
  }

  def picture = Auth { ctx ?=> _ =>
    AsStreamer: s =>
      Ok(html.streamer.picture(s)).noCache
  }

  private val ImageRateLimitPerIp = lila.memo.RateLimit.composite[lila.common.IpAddress](
    key = "streamer.image.ip"
  )(
    ("fast", 10, 2.minutes),
    ("slow", 30, 1.day)
  )

  def pictureApply = AuthBody(parse.multipartFormData) { ctx ?=> me =>
    AsStreamer: s =>
      ctx.body.body.file("picture") match
        case Some(pic) =>
          ImageRateLimitPerIp(ctx.ip, rateLimitedFu):
            api.uploadPicture(s.streamer, pic, me) recover { case e: Exception =>
              BadRequest(html.streamer.picture(s, e.getMessage.some))
            } inject Redirect(routes.Streamer.edit)
        case None => Redirect(routes.Streamer.edit).flashFailure
  }

  def subscribe(streamer: UserStr, set: Boolean) = AuthBody { _ ?=> me =>
    if set then env.relation.subs.subscribe(me.id, streamer.id)
    else env.relation.subs.unsubscribe(me.id, streamer.id)
    Ok
  }

  def onYouTubeVideo = AnonBodyOf(parse.tolerantXml):
    env.streamer.ytApi.onVideoXml

  def youTubePubSubChallenge = Anon:
    get("hub.challenge").fold(BadRequest): challenge =>
      val days      = get("hub.lease_seconds").map(s => f" for ${s.toFloat / (60 * 60 * 24)}%.1f days")
      val channelId = get("hub.topic").map(t => s" on ${t.split("=").last}")
      lila
        .log("streamer")
        .info(s"WebSub: CONFIRMED ${~get("hub.mode")}${~days}${~channelId}")
      Ok(challenge)

  private def AsStreamer(f: StreamerModel.WithContext => Fu[Result])(using ctx: WebContext): Fu[Result] =
    ctx.me.fold(notFound): me =>
      if (StreamerModel.canApply(me) || isGranted(_.Streamers))
        api.find(getUserStr("u").ifTrue(isGranted(_.Streamers)).map(_.id) | me.id) flatMap {
          _.fold(Ok(html.streamer.bits.create).toFuccess)(f)
        }
      else
        Ok:
          html.site.message("Too soon"):
            scalatags.Text.all.raw("You are not yet allowed to create a streamer profile.")

  private def WithVisibleStreamer(s: StreamerModel.WithContext)(f: Fu[Result])(using ctx: WebContext) =
    ctx.noKid.so:
      if (s.streamer.isListed || ctx.me.exists(_ is s.streamer) || isGranted(_.Admin)) f
      else notFound
