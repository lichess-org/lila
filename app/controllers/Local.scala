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

  def index(white: Option[String], black: Option[String], fen: Option[String], time: Option[String]) = Open:
    NoBot:
      Ok.page(views.local.index(lila.local.GameSetup(white, black, fen, time)))
        .map(_.enforceCrossSiteIsolation)
