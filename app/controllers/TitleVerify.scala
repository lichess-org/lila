package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.core.id.CmsPageKey

final class TitleVerify(env: Env, cmsC: => Cms) extends LilaController(env):

  def index = Auth { _ ?=> me ?=>
    cmsC.orCreateOrNotFound(CmsPageKey("title-verify-index")): page =>
      Ok.async(views.title.index(page))
  }

  def form = Auth { _ ?=> _ ?=>
    Ok.async(views.title.create(env.title.form.create))
  }

  def create = AuthBody { _ ?=> _ ?=>
    bindForm(env.title.form.create)(
      err => BadRequest.async(views.title.create(err)),
      data =>
        env.title.api
          .create(data)
          .map: req =>
            Redirect(routes.TitleVerify.show(req.id)).flashSuccess
    )
  }

  def show(id: String) = Auth { _ ?=> _ ?=>
    Found(env.title.api.getForMe(id)): req =>
      Ok.async(views.title.edit(env.title.form.edit(req.data), req))
  }

  def update(id: String) = SecureBody(_.Pages) { _ ?=> me ?=>
    Found(env.title.api.getForMe(id)): req =>
      bindForm(env.title.form.create)(
        err => BadRequest.async(views.title.edit(err, req)),
        data =>
          env.title.api
            .update(req, data)
            .inject:
              Redirect(routes.TitleVerify.show(req.id)).flashSuccess
      )
  }
