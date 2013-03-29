package controllers

import lila.api.Env.{ current => apiEnv }
import lila.api._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

object Cli extends LilaController {

  private lazy val form = Form(tuple(
    "command" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  def command = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    form.bindFromRequest.fold(
      err ⇒ fuccess(BadRequest("invalid cli call")), {
        case (command, password) ⇒ CliAuth(password) {
          apiEnv.cli(command.split(" ").toList.pp) map { res ⇒ Ok(res) }
        }
      })
  }

  private def CliAuth(password: String)(op: Fu[Result]): Fu[Result] =
    lila.user.UserRepo.checkPassword(apiEnv.CliUsername, password) flatMap {
      _.fold(op, fuccess(Unauthorized))
    }
}
