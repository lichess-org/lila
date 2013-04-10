package controllers

import lila.api._
import lila.user.Context

import play.api.mvc._, Results._
import play.api.libs.concurrent.Execution.Implicits._

object Lobby extends LilaController {

  def home = TODO

  def handleNotFound(req: RequestHeader): Fu[Result] =
    reqToCtx(req) flatMap { handleNotFound(_) }

  def handleNotFound(implicit ctx: Context): Fu[Result] =
    fuccess(NotFound)

}
