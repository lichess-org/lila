package controllers

import play.api.libs.json._
import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.streamer.{ Streamer => StreamerModel, StreamerForm }

final class Streamer(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def api = env.streamer.api

  def index(page: Int) =
    Open { implicit ctx =>
      ctx.noKid ?? {
        pageHit
        val requests = getBool("requests") && isGranted(_.Streamers)
        for {
          liveStreams <- env.streamer.liveStreamApi.all
          live        <- api withUsers liveStreams
          pager       <- env.streamer.pager.notLive(page, liveStreams, requests)
        } yield Ok(html.streamer.index(live, pager, requests))
      }
    }

  def featured = Action.async { implicit req =>
    env.streamer.liveStreamApi.all
      .map { streams =>
        val max      = env.streamer.homepageMaxSetting.get()
        val featured = streams.homepage(max) withTitles env.user.lightUserApi
        JsonOk {
          featured.live.streams.map { s =>
            Json.obj(
              "url"               -> routes.Streamer.redirect(s.streamer.id.value).absoluteURL(),
              "usernameWithTitle" -> featured.titleName(s),
              "status"            -> s.status
            )
          }
        }
      }
  }

  def live = apiC.ApiRequest { _ =>
    for {
      s     <- env.streamer.liveStreamApi.all
      users <- env.user.lightUserApi asyncManyFallback s.streams.map(_.streamer.userId)
    } yield apiC.toApiResult {
      (s.streams zip users).map { case (stream, user) =>
        lila.common.LightUser.lightUserWrites.writes(user) ++ lila.streamer.Stream.toJson(stream)
      }
    }
  }

  def show(username: String) =
    Open { implicit ctx =>
      OptionFuResult(api find username) { s =>
        WithVisibleStreamer(s) {
          for {
            sws      <- env.streamer.liveStreamApi of s
            activity <- env.activity.read.recent(sws.user)
          } yield Ok(html.streamer.show(sws, activity))
        }
      }
    }

  def redirect(username: String) =
    Open { implicit ctx =>
      OptionFuResult(api find username) { s =>
        WithVisibleStreamer(s) {
          env.streamer.liveStreamApi of s map { sws =>
            Redirect(sws.redirectToLiveUrl | routes.Streamer.show(username).url)
          }
        }
      }
    }

  def create =
    AuthBody { implicit ctx => me =>
      ctx.noKid ?? {
        NoLame {
          NoShadowban {
            api find me flatMap {
              case None => api.create(me) inject Redirect(routes.Streamer.edit)
              case _    => Redirect(routes.Streamer.edit).fuccess
            }
          }
        }
      }
    }

  private def modData(streamer: StreamerModel)(implicit ctx: Context) =
    isGranted(_.ModLog) ?? {
      env.mod.logApi.userHistory(streamer.userId) zip
        env.user.noteApi.forMod(streamer.userId) zip
        env.streamer.api.sameChannels(streamer) map some
    }

  def edit =
    Auth { implicit ctx => _ =>
      AsStreamer { s =>
        env.streamer.liveStreamApi of s flatMap { sws =>
          modData(s.streamer) map { forMod =>
            Ok(html.streamer.edit(sws, StreamerForm userForm sws.streamer, forMod)).noCache
          }
        }
      }
    }

  def editApply =
    AuthBody { implicit ctx => me =>
      AsStreamer { s =>
        env.streamer.liveStreamApi of s flatMap { sws =>
          implicit val req = ctx.body
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
                  if (change.decline) env.mod.logApi.streamerDecline(lila.report.Mod(me), s.user.id)
                  change.list foreach { env.mod.logApi.streamerList(lila.report.Mod(me), s.user.id, _) }
                  change.tier foreach { env.mod.logApi.streamerTier(lila.report.Mod(me), s.user.id, _) }
                  if (data.approval.flatMap(_.quick).isDefined)
                    env.streamer.pager.nextRequestId map { nextId =>
                      Redirect {
                        nextId.fold(s"${routes.Streamer.index()}?requests=1") { id =>
                          s"${routes.Streamer.edit.url}?u=$id"
                        }
                      }
                    }
                  else {
                    val next = if (sws.streamer is me) "" else s"?u=${sws.user.id}"
                    Redirect(s"${routes.Streamer.edit.url}${next}").fuccess
                  }
                }
            )
        }
      }
    }

  def approvalRequest =
    AuthBody { _ => me =>
      api.approval.request(me) inject Redirect(routes.Streamer.edit)
    }

  def picture =
    Auth { implicit ctx => _ =>
      AsStreamer { s =>
        Ok(html.streamer.picture(s)).noCache.fuccess
      }
    }

  def pictureApply =
    AuthBody(parse.multipartFormData) { implicit ctx => me =>
      AsStreamer { s =>
        ctx.body.body.file("picture") match {
          case Some(pic) =>
            (api.uploadPicture(s.streamer, pic, me) inject Redirect(routes.Streamer.edit)) recover {
              case e: Exception =>
                BadRequest(html.streamer.picture(s, e.getMessage.some))
            }
          case None => fuccess(Redirect(routes.Streamer.edit))
        }
      }
    }

  def pictureDelete =
    Auth { implicit ctx => _ =>
      AsStreamer { s =>
        api.deletePicture(s.streamer) inject Redirect(routes.Streamer.edit)
      }
    }

  private def AsStreamer(f: StreamerModel.WithUser => Fu[Result])(implicit ctx: Context) =
    ctx.me.fold(notFound) { me =>
      if (StreamerModel.canApply(me) || isGranted(_.Streamers))
        api.find(get("u").ifTrue(isGranted(_.Streamers)) | me.id) flatMap {
          _.fold(Ok(html.streamer.bits.create).fuccess)(f)
        }
      else
        Ok(
          html.site.message("Too soon")(
            scalatags.Text.all.raw("You are not yet allowed to create a streamer profile.")
          )
        ).fuccess
    }

  private def WithVisibleStreamer(s: StreamerModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    ctx.noKid ?? {
      if (s.streamer.isListed || ctx.me.??(s.streamer.is) || isGranted(_.Admin)) f
      else notFound
    }
}
