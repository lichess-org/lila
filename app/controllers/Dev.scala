package controllers

import play.api.data._, Forms._
import views._

import lila.app._

final class Dev(env: Env) extends LilaController(env) {

  private lazy val settingsList = List[lila.memo.SettingStore[_]](
    env.security.ugcArmedSetting,
    env.security.spamKeywordsSetting,
    env.oAuth.originBlocklistSetting,
    env.mailer.mailerSecondaryPermilleSetting,
    env.irwin.irwinApi.thresholds,
    env.irwin.kaladinApi.thresholds,
    env.explorer.indexFlowSetting,
    env.report.scoreThresholdsSetting,
    env.report.discordScoreThresholdSetting,
    env.round.selfReportEndGame,
    env.round.selfReportMarkUser,
    env.streamer.homepageMaxSetting,
    env.streamer.alwaysFeaturedSetting,
    env.rating.ratingFactorsSetting,
    env.plan.donationGoalSetting,
    env.plan.paymentMethodsSetting,
    env.apiTimelineSetting,
    env.apiExplorerGamesPerSecond,
    env.fishnet.openingBookDepth,
    env.noDelaySecretSetting,
    env.featuredTeamsSetting,
    env.prizeTournamentMakers,
    env.pieceImageExternal,
    env.evalCache.enable,
    env.tournament.reloadEndpointSetting,
    env.tutor.nbAnalysisSetting
  )

  def settings =
    Secure(_.Settings) { implicit ctx => _ =>
      Ok(html.dev.settings(settingsList)).fuccess
    }

  def settingsPost(id: String) =
    SecureBody(_.Settings) { implicit ctx => me =>
      settingsList.find(_.id == id) ?? { setting =>
        implicit val req = ctx.body
        setting.form
          .bindFromRequest()
          .fold(
            _ => BadRequest(html.dev.settings(settingsList)).fuccess,
            v => {
              lila
                .log("setting")
                .info(s"${me.user.username} changes $id from ${setting.get()} to ${v.toString}")
              setting.setString(v.toString) inject Redirect(routes.Dev.settings)
            }
          )
      }
    }

  private val commandForm = Form(single("command" -> nonEmptyText))

  def cli =
    Secure(_.Cli) { implicit ctx => _ =>
      Ok(html.dev.cli(commandForm, none)).fuccess
    }

  def cliPost =
    SecureBody(_.Cli) { implicit ctx => me =>
      implicit val req = ctx.body
      commandForm
        .bindFromRequest()
        .fold(
          err => BadRequest(html.dev.cli(err, "Invalid command".some)).fuccess,
          command =>
            runAs(me.id, command) map { res =>
              Ok(html.dev.cli(commandForm fill command, s"$command\n\n$res".some))
            }
        )
    }

  def command =
    ScopedBody(parse.tolerantText)(Seq(_.Preference.Write)) { implicit req => me =>
      lila.security.Granter(_.Cli)(me) ?? {
        runAs(me.id, req.body) map { Ok(_) }
      }
    }

  private def runAs(user: lila.user.User.ID, command: String): Fu[String] =
    env.mod.logApi.cli(user, command) >>
      env.api.cli(command.split(" ").toList)
}
