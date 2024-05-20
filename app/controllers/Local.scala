package controllers

import play.api.libs.json.*
import play.api.i18n.Lang
import play.api.mvc.*
import play.api.data.*
import play.api.data.Forms.*
import views.*

import lila.app.{ given, * }
import lila.common.Json.given
import lila.user.User
import lila.rating.{ Perf, PerfType }
import lila.local.GameSetup

final class Local(env: Env) extends LilaController(env):
  val setupForm =
    Form:
      mapping(
        "white" -> optional(nonEmptyText),
        "black" -> optional(nonEmptyText),
        "fen"   -> optional(nonEmptyText),
        "time"  -> optional(nonEmptyText)
      )(GameSetup.apply)(unapply)

  def game(testUi: Boolean = false) = OpenBody:
    NoBot:
      bindForm(setupForm)(
        err => jsonFormError(err),
        setup =>
          Ok.page(
            views.local.game(
              Json.obj("pref" -> lila.pref.JsonView.write(ctx.pref, false)),
              setup,
              testUi
            )
          ).map(_.enforceCrossSiteIsolation)
      )
