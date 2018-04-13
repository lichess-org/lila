package controllers

import play.api.data._, Forms._
import play.api.mvc._

import lila.app._
import views._

object Dev extends LilaController {

  private lazy val settingsList = List[lila.memo.SettingStore[_]](
    Env.security.ugcArmedSetting,
    Env.security.emailBlacklistSetting,
    Env.irwin.irwinModeSetting,
    Env.api.assetVersionSetting,
    Env.explorer.indexFlowSetting,
    Env.report.scoreThresholdSetting,
    Env.api.roundRouterSetting
  )

  def settings = Secure(_.Settings) { implicit ctx => me =>
    Ok(html.dev.settings(settingsList)).fuccess
  }

  def settingsPost(id: String) = SecureBody(_.Settings) { implicit ctx => me =>
    ~(for {
      setting <- settingsList.find(_.id == id)
      form <- setting.form
    } yield {
      implicit val req = ctx.body
      form.bindFromRequest.fold(
        err => BadRequest(html.dev.settings(settingsList)).fuccess,
        v => setting.setString(v.toString) inject Redirect(routes.Dev.settings)
      )
    })
  }

  private val commandForm = Form(single(
    "command" -> nonEmptyText
  ))

  def cli = Secure(_.Cli) { implicit ctx => me =>
    Ok(html.dev.cli(commandForm, none)).fuccess
  }

  def cliPost = SecureBody(_.Cli) { implicit ctx => me =>
    implicit val req = ctx.body
    commandForm.bindFromRequest.fold(
      err => BadRequest(html.dev.cli(err, "Invalid command".some)).fuccess,
      command => runAs(me.id, command) map { res =>
        Ok(html.dev.cli(commandForm fill command, s"$command\n\n$res".some))
      }
    )
  }

  private lazy val loginForm = Form(tuple(
    "command" -> nonEmptyText,
    "password" -> nonEmptyText
  ))

  def command = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    loginForm.bindFromRequest.pp.fold(
      err => fuccess(BadRequest("invalid cli call")), {
        case (command, password) => CommandAuth(password) {
          runAs(Env.api.CliUsername, command) map { res => Ok(res) }
        }
      }
    )
  }

  private def runAs(user: lila.user.User.ID, command: String): Fu[String] =
    Env.mod.logApi.cli(user, command) >>
      Env.api.cli(command.split(" ").toList)

  private def CommandAuth(password: String)(op: => Fu[Result]): Fu[Result] =
    Env.user.authenticator.authenticateById(
      Env.api.CliUsername,
      lila.user.User.ClearPassword(password)
    ).map(_.isDefined) flatMap {
        _.fold(op, fuccess(Unauthorized))
      }
}
