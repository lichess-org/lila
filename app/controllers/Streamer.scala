package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.streamer.{ Streamer => StreamerModel, StreamerForm }
import views._

object Streamer extends LilaController {

  private def api = Env.streamer.api

  def index(page: Int) = Open { implicit ctx =>
    pageHit
    val requests = getBool("requests") && isGranted(_.Streamers)
    for {
      liveStreams <- Env.streamer.liveStreamApi.all
      live <- api withUsers liveStreams
      pager <- Env.streamer.pager.notLive(page, liveStreams, requests)
    } yield Ok(html.streamer.index(live, pager, requests))
  }

  def live = Api.ApiRequest { implicit ctx =>
    Env.user.lightUserApi asyncMany Env.streamer.liveStreamApi.userIds.toList dmap (_.flatten) map { users =>
      val playingIds = Env.relation.online.playing intersect users.map(_.id)
      Api.toApiResult {
        users.map { u =>
          lila.common.LightUser.lightUserWrites.writes(u).add("playing" -> playingIds(u.id))
        }
      }
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionFuResult(api find username) { s =>
      WithVisibleStreamer(s) {
        for {
          sws <- Env.streamer.liveStreamApi of s
          activity <- Env.activity.read.recent(sws.user, 10)
          following <- ctx.userId.??(Env.relation.api.fetchFollows(_, sws.user.id))
        } yield Ok(html.streamer.show(sws, activity, following))
      }
    }
  }

  def create = AuthBody { implicit ctx => me =>
    NoLame {
      NoShadowban {
        api find me flatMap {
          case None => api.create(me) inject Redirect(routes.Streamer.edit)
          case _ => Redirect(routes.Streamer.edit).fuccess
        }
      }
    }
  }

  private def modData(user: lila.user.User)(implicit ctx: Context) = isGranted(_.ModLog) ?? {
    Env.mod.logApi.userHistory(user.id) zip
      Env.user.noteApi.forMod(user.id) map some
  }

  def edit = Auth { implicit ctx => me =>
    AsStreamer { s =>
      Env.streamer.liveStreamApi of s flatMap { sws =>
        modData(s.user) map { forMod =>
          NoCache(Ok(html.streamer.edit(sws, StreamerForm userForm sws.streamer, forMod)))
        }
      }
    }
  }

  def editApply = AuthBody { implicit ctx => me =>
    AsStreamer { s =>
      Env.streamer.liveStreamApi of s flatMap { sws =>
        implicit val req = ctx.body
        StreamerForm.userForm(sws.streamer).bindFromRequest.fold(
          error => modData(s.user) map { forMod =>
            BadRequest(html.streamer.edit(sws, error, forMod))
          },
          data => api.update(sws.streamer, data, isGranted(_.Streamers)) map { change =>
            change.list foreach { Env.mod.logApi.streamerList(lila.report.Mod(me), s.user.id, _) }
            change.feature foreach { Env.mod.logApi.streamerFeature(lila.report.Mod(me), s.user.id, _) }
            Redirect {
              s"${routes.Streamer.edit().url}${if (sws.streamer is me) "" else "?u=" + sws.user.id}"
            }
          }
        )
      }
    }
  }

  def approvalRequest = AuthBody { implicit ctx => me =>
    api.approval.request(me) inject Redirect(routes.Streamer.edit)
  }

  def picture = Auth { implicit ctx => _ =>
    AsStreamer { s =>
      NoCache(Ok(html.streamer.picture(s))).fuccess
    }
  }

  def pictureApply = AuthBody(BodyParsers.parse.multipartFormData) { implicit ctx => _ =>
    AsStreamer { s =>
      ctx.body.body.file("picture") match {
        case Some(pic) => api.uploadPicture(s.streamer, pic) recover {
          case e: lila.base.LilaException => BadRequest(html.streamer.picture(s, e.message.some))
        } inject Redirect(routes.Streamer.edit)
        case None => fuccess(Redirect(routes.Streamer.edit))
      }
    }
  }

  def pictureDelete = Auth { implicit ctx => _ =>
    AsStreamer { s =>
      api.deletePicture(s.streamer) inject Redirect(routes.Streamer.edit)
    }
  }

  private def AsStreamer(f: StreamerModel.WithUser => Fu[Result])(implicit ctx: Context) =
    ctx.me.fold(notFound) { me =>
      api.find(get("u").ifTrue(isGranted(_.Streamers)) | me.id) flatMap {
        _.fold(Ok(html.streamer.bits.create(me)).fuccess)(f)
      }
    }

  private def WithVisibleStreamer(s: StreamerModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    if (s.streamer.isListed || ctx.me.??(s.streamer.is) || isGranted(_.Admin)) f
    else notFound
}
