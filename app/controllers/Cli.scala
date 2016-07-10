package controllers

import lila.app._

import play.api.mvc._, Results._
import play.api.data._, Forms._

object Cli extends LilaController {

  private lazy val form = Form(tuple(
    "command" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  def command = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    form.bindFromRequest.fold(
      err => fuccess(BadRequest("invalid cli call")), {
        case (command, password) => CliAuth(password) {
          Env.api.cli(command.split(" ").toList) map { res => Ok(res) }
        }
      })
  }

  private def CliAuth(password: String)(op: => Fu[Result]): Fu[Result] =
    lila.user.UserRepo.authenticateById(Env.api.CliUsername, password).map(_.isDefined) flatMap {
      _.fold(op, fuccess(Unauthorized))
    }
}
