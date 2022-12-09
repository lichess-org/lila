package controllers

import play.api.data.*, Forms.*
import views.*

import lila.app.{ given, * }

final class Dev(env: Env) extends LilaController(env):

  private lazy val settingsList = List[lila.memo.SettingStore[?]](
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
      Ok(html.dev.settings(settingsList)).toFuccess
    }

  def settingsPost(id: String) =
    SecureBody(_.Settings) { implicit ctx => me =>
      settingsList.find(_.id == id) ?? { setting =>
        given play.api.mvc.Request[?] = ctx.body
        setting.form
          .bindFromRequest()
          .fold(
            _ => BadRequest(html.dev.settings(settingsList)).toFuccess,
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
      Ok(html.dev.cli(commandForm, none)).toFuccess
    }

  def cliPost =
    SecureBody(_.Cli) { implicit ctx => me =>
      given play.api.mvc.Request[?] = ctx.body
      commandForm
        .bindFromRequest()
        .fold(
          err => BadRequest(html.dev.cli(err, "Invalid command".some)).toFuccess,
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

  private def runAs(user: UserId, command: String): Fu[String] =
    env.mod.logApi.cli(user into ModId, command) >>
      env.api.cli(command.split(" ").toList)
