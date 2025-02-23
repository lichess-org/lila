package controllers

import lila.app.*

final class Dev(env: Env) extends LilaController(env):

  private lazy val settingsList = List[lila.memo.SettingStore[?]](
    env.security.ugcArmedSetting,
    env.security.spamKeywordsSetting,
    env.security.proxy2faSetting,
    env.oAuth.originBlocklistSetting,
    env.mailer.mailerSecondaryPermilleSetting,
    env.irwin.irwinApi.thresholds,
    env.irwin.kaladinApi.thresholds,
    env.report.scoreThresholdsSetting,
    env.report.discordScoreThresholdSetting,
    env.round.selfReportEndGame,
    env.round.selfReportMarkUser,
    env.bot.boardReport.domainSetting,
    env.streamer.homepageMaxSetting,
    env.streamer.alwaysFeaturedSetting,
    env.round.ratingFactorsSetting,
    env.plan.donationGoalSetting,
    env.fishnet.openingBookDepth,
    env.web.settings.apiTimeline,
    env.web.settings.apiExplorerGamesPerSecond,
    env.web.settings.noDelaySecret,
    env.web.settings.prizeTournamentMakers,
    env.web.settings.sitewideCoepCredentiallessHeader,
    env.tournament.reloadEndpointSetting,
    env.tutor.nbAnalysisSetting,
    env.tutor.parallelismSetting,
    env.recap.parallelismSetting,
    env.relay.proxyDomainRegex,
    env.relay.proxyHostPort,
    env.relay.proxyCredentials
  )

  def settings = Secure(_.Settings) { _ ?=> _ ?=>
    Ok.page:
      views.dev.settings(settingsList)
  }

  def settingsPost(id: String) = SecureBody(_.Settings) { _ ?=> me ?=>
    settingsList.find(_.id == id).so { setting =>
      bindForm(setting.form)(
        _ => BadRequest.page(views.dev.settings(settingsList)),
        v =>
          lila
            .log("setting")
            .info(s"${me.username} changes $id from ${setting.get()} to ${v.toString}")
          setting.setString(v.toString).inject(Redirect(routes.Dev.settings))
      )
    }
  }

  def cli = Secure(_.Cli) { _ ?=> _ ?=>
    Ok.page:
      views.dev.cli(env.api.cli.form, none)
  }

  def cliPost = SecureBody(_.Cli) { _ ?=> me ?=>
    bindForm(env.api.cli.form)(
      err => BadRequest.page(views.dev.cli(err, "Invalid command".some)),
      command =>
        Ok.async:
          runCommand(command).map: res =>
            views.dev.cli(env.api.cli.form.fill(command), s"$command\n\n$res".some)
    )
  }

  def command = ScopedBody(parse.tolerantText)(Seq(_.Preference.Write)) { ctx ?=> _ ?=>
    isGranted(_.Cli).so:
      runCommand(ctx.body.body).map { Ok(_) }
  }

  private def runCommand(command: String)(using Me): Fu[String] =
    env.mod.logApi.cli(command) >>
      env.api.cli(command.split(" ").toList)

end Dev
