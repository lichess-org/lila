package controllers

import play.api.libs.json.*

import lila.app.{ *, given }
import lila.common.Json.given
import lila.core.id.CmsPageKey

final class TitleVerify(env: Env) extends LilaController(env):

  def index = Auth { _ ?=> me ?=>
    Found(env.api.cmsRender(CmsPageKey("title-verify-index"))): p =>
      Ok.async(views.site.page.lone(p))
  }

  def form = Auth { _ ?=> _ ?=>
    Ok.async(views.title.create(env.title.form.create))
  }

  def create = AuthBody { _ ?=> _ ?=>
    ???
  }
