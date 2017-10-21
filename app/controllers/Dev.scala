package controllers

import play.api.data._, Forms._

import lila.app._
import views._

object Dev extends LilaController {

  def assetVersion = Secure(_.AssetVersion) { implicit ctx => me =>
    Ok(html.dev.assetVersion(
      Env.api.assetVersion.fromConfig,
      Env.api.assetVersion.get
    )).fuccess
  }

  def assetVersionPost = SecureBody(_.AssetVersion) { implicit ctx => me =>
    implicit val req = ctx.body
    Form(single(
      "version" -> number(min = 0)
    )).bindFromRequest.fold(
      err => funit,
      v => Env.api.assetVersion.set(
        lila.common.AssetVersion(v)
      ) inject Redirect(routes.Dev.assetVersion)
    ) inject Redirect(routes.Dev.assetVersion)
  }
}
