package controllers

import lila.app.*

final class Dev(env: Env) extends LilaController(env):

  def settings = Secure(_.Settings) { _ ?=> _ ?=>
    Ok.page:
      views.dev.settings(settingsList)
  }

  def settingsPost(id: String) = SecureBody(_.Settings) { _ ?=> me ?=>
    settingsList.flatMap(_._2).find(_.id == id).so { setting =>
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

  def ipTiers = Secure(_.IpTiers) { ctx ?=> _ ?=>
    env.security.ipTiers.form.flatMap: form =>
      Ok.page(views.dev.ipTiers(form))
  }

  def ipTiersPost = SecureBody(_.IpTiers) { ctx ?=> _ ?=>
    Found(env.security.ipTiers.form.map(_.toOption)): form =>
      bindForm(form)(
        err => BadRequest.page(views.dev.ipTiers(Right(err))),
        v => env.security.ipTiers.writeToFile(v).inject(Redirect(routes.Dev.ipTiers).flashSuccess)
      )
  }

  private def runCommand(command: String)(using Me): Fu[String] =
    for
      _ <- env.mod.logApi.cli(command)
      res <- env.api.cli.run(command.split(" ").toList)
    yield res

  private lazy val settingsList = List[(String, List[lila.memo.SettingStore[?]])](
    "Moderation" -> List(
      env.security.ugcArmedSetting,
      env.security.spamKeywordsSetting,
      env.irwin.irwinApi.thresholds,
      env.irwin.kaladinApi.thresholds,
      env.report.scoreThresholdsSetting,
      env.report.discordScoreThresholdSetting
    ),
    "Cheat" -> List(
      env.round.selfReportEndGame,
      env.round.selfReportMarkUser,
      env.bot.boardReport.domainSetting
    ),
    "Security" -> List(
      env.oAuth.originBlocklistSetting,
      env.security.proxy2faSetting,
      env.security.alwaysCaptcha
    ),
    "Mailing" -> List(
      env.mailer.mailerSecondaryPermilleSetting,
      env.mailer.canSendEmailsSetting
    ),
    "Streamer" -> List(
      env.streamer.homepageMaxSetting,
      env.streamer.alwaysFeaturedSetting
    ),
    "Permissions" -> List(
      env.web.settings.noDelaySecret,
      env.web.settings.prizeTournamentMakers
    ),
    "Limits" -> List(
      env.web.settings.apiTimeline,
      env.web.settings.apiExplorerGamesPerSecond,
      env.tutor.nbAnalysisSetting,
      env.tutor.parallelismSetting,
      env.recap.parallelismSetting,
      env.fishnet.openingBookDepth
    ),
    "Broadcast" -> List(
      env.relay.proxyDomainRegex,
      env.relay.proxyHostPort,
      env.relay.proxyCredentials
    ),
    "Automod" -> List(
      env.report.automod.imageModelSetting,
      env.report.automod.imagePromptSetting,
      env.report.api.commsModelSetting,
      env.report.api.commsPromptSetting,
      env.ublog.ublogAutomod.modelSetting,
      env.ublog.ublogAutomod.promptSetting
    ),
    "Mobile" -> List(
      env.web.mobile.androidVersion,
      env.web.mobile.iosVersion
    ),
    "Config" -> List(
      env.plan.donationGoalSetting,
      env.tournament.reloadEndpointSetting
    )
  )
