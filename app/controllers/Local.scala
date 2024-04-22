package controllers

import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.Json.given
import lila.user.User
import play.api.i18n.Lang
import lila.rating.{ Perf, PerfType }

final class Local(env: Env) extends LilaController(env):

  def vsBot = Open:
    NoBot:
      Ok.page(views.html.local.vsBot.index).map(_.enforceCrossSiteIsolation)

  def botVsBot = Open:
    NoBot:
      Ok.page(views.html.local.botVsBot.index).map(_.enforceCrossSiteIsolation)

  def setup = Open:
    NoBot:
      Ok
