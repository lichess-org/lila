package controllers

import play.api.data._, Forms._
import play.api.mvc._

import lila.app._
import views._

import lila.user.User.{ ClearPassword, TotpToken, PasswordAndToken }

object Dev extends LilaController {

  private lazy val settingsList = List[lila.memo.SettingStore[_]](
    Env.security.ugcArmedSetting,
    Env.security.emailBlacklistSetting,
    Env.security.spamKeywordsSetting,
    Env.irwin.irwinModeSetting,
    Env.explorer.indexFlowSetting,
    Env.report.scoreThresholdSetting,
    Env.api.cspEnabledSetting,
    Env.streamer.alwaysFeaturedSetting,
    Env.rating.ratingFactorsSetting,
    Env.socket.socketDebugSetting,
    Env.socket.socketRemoteUsersSetting,
    Env.api.serviceWorkerSetting,
    Env.plan.donationGoalSetting,
    Env.rating.deviationIncreaseOverTimeSetting
  )

  def settings = Secure(_.Settings) { implicit ctx => me =>
    Ok(html.dev.settings(settingsList)).fuccess
  }

  def settingsPost(id: String) = SecureBody(_.Settings) { implicit ctx => me =>
    settingsList.find(_.id == id) ?? { setting =>
      implicit val req = ctx.body
      setting.form.bindFromRequest.fold(
        err => BadRequest(html.dev.settings(settingsList)).fuccess,
        v => setting.setString(v.toString) inject Redirect(routes.Dev.settings)
      )
    }
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

  def command = ScopedBody(parse.tolerantText)(Seq(_.Preference.Write)) { implicit req => me =>
    lila.security.Granter(_.Cli)(me) ?? {
      runAs(me.id, req.body) map { Ok(_) }
    }
  }

  private def runAs(user: lila.user.User.ID, command: String): Fu[String] =
    Env.mod.logApi.cli(user, command) >>
      Env.api.cli(command.split(" ").toList)
}
