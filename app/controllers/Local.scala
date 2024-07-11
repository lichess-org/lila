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
        "time"  -> optional(nonEmptyText),
        "go"    -> play.api.data.Forms.boolean
      )(GameSetup.apply)(unapply)

  def index(devUi: Option[String]) = OpenBody:
    NoBot:
      Ok.page(indexPage(none, optTrue(devUi))).map(_.enforceCrossSiteIsolation)

  def newGame(
      white: Option[String],
      black: Option[String],
      fen: Option[String],
      time: Option[String],
      go: Option[String],
      devUi: Option[String]
  ) = OpenBody:
    NoBot:
      Ok.page(indexPage(GameSetup(white, black, fen, time, optTrue(go)).some, optTrue(devUi)))
        .map(_.enforceCrossSiteIsolation)

  def newGameForm(devUi: Option[String]) = OpenBody:
    NoBot:
      bindForm(setupForm)(
        err => jsonFormError(err),
        setup => Ok.page(indexPage(setup.some, optTrue(devUi))).map(_.enforceCrossSiteIsolation)
      )

  private def indexPage(setup: Option[GameSetup], devUi: Boolean)(using ctx: Context) =
    given setupFormat: Format[GameSetup] = Json.format[GameSetup]
    views.local.index(
      Json
        .obj(
          "pref" -> pref
        )
        .add("setup", setup)
        .add(
          "assets",
          devUi.option(
            // we'll replace these filesystem calls with a mapping json when things settle down
            Json.obj(
              "image" -> env.getFile(s"public/lifat/bots/images").listFiles().toList.map(_.getName),
              "net"   -> env.getFile(s"public/lifat/bots/nets").listFiles().toList.map(_.getName),
              "book"  -> env.getFile(s"public/lifat/bots/books").listFiles().toList.map(_.getName),
              "sound" -> env.getFile(s"public/lifat/bots/sounds").listFiles().toList.map(_.getName)
            )
          )
        ),
      devUi
    )

  private def pref(using ctx: Context) =
    lila.pref.JsonView.write(ctx.pref, false).add("animationDuration", ctx.pref.animationMillis.some)

  private def optTrue(s: Option[String]) =
    s.exists(v => v == "" || v == "1" || v == "true")
