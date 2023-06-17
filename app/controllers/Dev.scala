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
    env.report.scoreThresholdsSetting,
    env.report.discordScoreThresholdSetting,
    env.round.selfReportEndGame,
    env.round.selfReportMarkUser,
    env.streamer.homepageMaxSetting,
    env.streamer.alwaysFeaturedSetting,
    env.rating.ratingFactorsSetting,
    env.plan.donationGoalSetting,
    env.apiTimelineSetting,
    env.apiExplorerGamesPerSecond,
    env.fishnet.openingBookDepth,
    env.noDelaySecretSetting,
    env.featuredTeamsSetting,
    env.prizeTournamentMakers,
    env.pieceImageExternal,
    env.tournament.reloadEndpointSetting,
    env.tutor.nbAnalysisSetting,
    env.tutor.parallelismSetting,
    env.firefoxOriginTrial,
    env.credentiallessUaRegex
  )

  def settings = Secure(_.Settings) { _ ?=> _ =>
    html.dev.settings(settingsList)
  }

  def settingsPost(id: String) = SecureBody(_.Settings) { _ ?=> me =>
    settingsList.find(_.id == id) so { setting =>
      setting.form
        .bindFromRequest()
        .fold(
          _ => BadRequest(html.dev.settings(settingsList)),
          v =>
            lila
              .log("setting")
              .info(s"${me.user.username} changes $id from ${setting.get()} to ${v.toString}")
            setting.setString(v.toString) inject Redirect(routes.Dev.settings)
        )
    }
  }

  private val commandForm = Form(single("command" -> nonEmptyText))

  def cli = Secure(_.Cli) { _ ?=> _ =>
    html.dev.cli(commandForm, none)
  }

  def cliPost = SecureBody(_.Cli) { _ ?=> me =>
    commandForm
      .bindFromRequest()
      .fold(
        err => BadRequest(html.dev.cli(err, "Invalid command".some)),
        command =>
          runAs(me.id, command) map { res =>
            Ok(html.dev.cli(commandForm fill command, s"$command\n\n$res".some))
          }
      )
  }

  def command = ScopedBody(parse.tolerantText)(Seq(_.Preference.Write)) { ctx ?=> me =>
    lila.security.Granter(_.Cli)(me) so {
      runAs(me.id, ctx.body.body) map { Ok(_) }
    }
  }

  private def runAs(user: UserId, command: String): Fu[String] =
    env.mod.logApi.cli(user into ModId, command) >>
      env.api.cli(command.split(" ").toList)
