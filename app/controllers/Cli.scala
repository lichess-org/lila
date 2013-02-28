package controllers

import lila.app._
import views._

import play.api.mvc._
import play.api.mvc.Results._
import play.api.data._
import play.api.data.Forms._

import scalaz.effects._

object Cli extends LilaController {

  private def userRepo = env.user.userRepo
  private def runCommand = lila.cli.Main.main(env) _

  private lazy val form = Form(tuple(
    "command" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  def command = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    IOResult {
      form.bindFromRequest.fold(
        err ⇒ io(Ok("bad request")), {
          case (command, password) ⇒ CliAuth(password) {
            runCommand(command.split(" ")) map { res ⇒ Ok(res) }
          }
        })
    }
  }

  private def CliAuth(password: String)(op: IO[Result]): IO[Result] =
    userRepo.checkPassword(env.settings.CliUsername, password) flatMap { 
      _.fold(op, io(Ok("permission denied")))
    }
}
