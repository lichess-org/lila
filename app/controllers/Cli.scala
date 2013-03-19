package controllers

import lila.api._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._

object Cli extends LilaController {

  private def userRepo = env.user.userRepo
  private def runCommand = lila.api.Cli(env) _

  private lazy val form = Form(tuple(
    "command" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  def command = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    form.bindFromRequest.fold(
      err ⇒ fuccess(Ok("bad request")), {
        case (command, password) ⇒ CliAuth(password) {
          runCommand(command.split(" ").toList.pp) map { res ⇒ Ok(res) }
        }
      })
  }

  private def CliAuth(password: String)(op: Fu[Result]): Fu[Result] =
    userRepo.checkPassword(env.settings.Cli.Username, password) flatMap {
      _.fold(op, fuccess(Ok("permission denied")))
    }
}
