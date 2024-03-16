package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.Json.given
import lila.common.config.MaxPerSecond
import lila.user.User
import lila.common.LangPath
import play.api.i18n.Lang
import lila.rating.{ Perf, PerfType }

final class LocalPlay(env: Env) extends LilaController(env):

  def vsBot = Open:
    NoBot:
      Ok.page(views.html.localPlay.vsBot.index).map(_.enforceCrossSiteIsolation)

  def botVsBot = Open:
    NoBot:
      Ok.page(views.html.localPlay.botVsBot.index).map(_.enforceCrossSiteIsolation)

  def setup = Open:
    NoBot:
      Ok
