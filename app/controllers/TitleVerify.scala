package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.title.TitleRequest
import lila.core.id.{ TitleRequestId, CmsPageKey }

final class TitleVerify(env: Env, cmsC: => Cms) extends LilaController(env):

  private def api = env.title.api

  def index = Auth { _ ?=> me ?=>
    cmsC.orCreateOrNotFound(CmsPageKey("title-verify-index")): page =>
      api.getCurrent.flatMap:
        case Some(req) => Redirect(routes.TitleVerify.show(req.id))
        case None      => Ok.async(views.title.index(page.title, views.site.page.pageContent(page)))
  }

  def form = Auth { _ ?=> _ ?=>
    Ok.async(views.title.create(env.title.form.create))
  }

  def create = AuthBody { _ ?=> _ ?=>
    bindForm(env.title.form.create)(
      err => BadRequest.async(views.title.create(err)),
      data =>
        api
          .create(data)
          .map: req =>
            Redirect(routes.TitleVerify.show(req.id))
    )
  }

  def show(id: TitleRequestId) = Auth { _ ?=> _ ?=>
    Found(api.getForMe(id)): req =>
      Ok.async(views.title.edit(env.title.form.edit(req.data), req))
  }

  def update(id: TitleRequestId) = SecureBody(_.Pages) { _ ?=> me ?=>
    Found(api.getForMe(id)): req =>
      bindForm(env.title.form.create)(
        err => BadRequest.async(views.title.edit(err, req)),
        data =>
          api
            .update(req, data)
            .map: req =>
              val redir = Redirect(routes.TitleVerify.show(req.id))
              if req.status == TitleRequest.Status.building then redir
              else redir.flashSuccess
      )
  }

  def cancel(id: TitleRequestId) = SecureBody(_.Pages) { _ ?=> me ?=>
    Found(api.getForMe(id)): req =>
      api
        .delete(req)
        .inject:
          Redirect(routes.TitleVerify.index).flashSuccess
  }

  def image(id: TitleRequestId, tag: String) = AuthBody(parse.multipartFormData) { ctx ?=> me ?=>
    Found(api.getForMe(id)): req =>
      ctx.body.body.file("image") match
        case Some(image) =>
          limit.imageUpload(ctx.ip, rateLimited):
            api.image
              .upload(req, image, tag)
              .inject(Ok)
              .recover { case e: Exception =>
                BadRequest(e.getMessage)
              }
        case None => api.image.delete(req, tag) >> Ok
  }
