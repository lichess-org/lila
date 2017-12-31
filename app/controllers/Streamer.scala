package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.streamer.{ Streamer => StreamerModel, StreamerForm }
import views._

object Streamer extends LilaController {

  private def api = Env.streamer.api

  def index(page: Int) = Open { implicit ctx =>
    Env.streamer.pager(page) map { pager =>
      Ok(html.streamer.index(pager))
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionFuResult(api find username) { s =>
      WithVisibleStreamer(s) {
        Ok(html.streamer.show(s)).fuccess
      }
    }
  }

  def create = Auth { implicit ctx => me =>
    NoLame {
      NoShadowban {
        Ok(html.streamer.create(me)).fuccess
      }
    }
  }

  def createApply = AuthBody { implicit ctx => me =>
    NoLame {
      NoShadowban {
        api.create(me) inject Redirect(routes.Streamer.edit)
      }
    }
  }

  def edit = Auth { implicit ctx => me =>
    AsStreamer { s =>
      NoCache(Ok(html.streamer.edit(s, StreamerForm userForm s.streamer))).fuccess
    }
  }

  def editApply = AuthBody { implicit ctx => _ =>
    AsStreamer { s =>
      implicit val req = ctx.body
      StreamerForm.userForm(s.streamer).bindFromRequest.fold(
        error => BadRequest(html.streamer.edit(s, error)).fuccess,
        data => api.update(s.streamer, data, isGranted(_.Streamers)) inject Redirect(routes.Streamer.edit())
      )
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
    get("u").ifTrue(isGranted(_.Streamers)).orElse(ctx.userId) ?? api.find flatMap { _.fold(notFound)(f) }

  private def WithVisibleStreamer(s: StreamerModel.WithUser)(f: Fu[Result])(implicit ctx: Context) =
    if (s.streamer.isListed || ctx.me.??(s.streamer.is) || isGranted(_.Admin)) f
    else notFound
}
