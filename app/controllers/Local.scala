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
import lila.local.{ GameSetup, AssetType }

final class Local(env: Env) extends LilaController(env):

  def index(
      white: Option[String],
      black: Option[String],
      fen: Option[String],
      time: Option[String],
      go: Option[String]
  ) = OpenBody:
    val initial   = time.map(_.toFloat)
    val increment = time.flatMap(_.split('+').drop(1).headOption.map(_.toFloat))
    val setup =
      if white.isDefined || black.isDefined || fen.isDefined || time.isDefined then
        GameSetup(white, black, fen, initial, increment, optTrue(go)).some
      else none
    for
      bots <- env.local.repo.getLatest()
      page <- renderPage(indexPage(setup, bots, false))
    yield Ok(page).enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")

  def dev = AuthBody: _ ?=>
    for
      bots <- env.local.repo.getLatest()
      page <- renderPage(indexPage(none, bots, true))
    yield Ok(page).enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")

  def assetList = Open: ctx ?=> // for service worker
    localOk(env.local.api.assets)

  def botList = Open: ctx ?=>
    env.local.repo
      .getLatest()
      .map: bots =>
        localOk(Json.obj("bots" -> bots))

  // for bot devs
  def botHistory = AuthBody: ctx ?=>
    env.local.repo
      .getAll()
      .map: history =>
        localOk(Json.obj("bots" -> history))

  // for bot devs
  def postBot = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.pp.body.body
      .validate[JsObject]
      .fold(
        err => BadRequest(Json.obj("error" -> err.toString)),
        bot =>
          env.local.repo
            .put(bot, me.userId)
            .map: updatedBot =>
              localOk(updatedBot)
      )
  }

  // for bot devs
  def postAsset(tpe: String, name: String) = Action.async(parse.multipartFormData): request =>
    val assetType: Option[AssetType] = tpe match
      case "image" => "image".some
      case "net"   => "net".some
      case "book"  => "book".some
      case "sound" => "sound".some
      case _       => none
    assetType match
      case Some(tpe) =>
        request.body
          .file("file")
          .map: file =>
            env.local.api
              .storeAsset(tpe, name, file)
              .map:
                case Left(error)   => InternalServerError(Json.obj("error" -> error.toString)).as(JSON)
                case Right(assets) => localOk(assets)
          .getOrElse(fuccess(BadRequest(Json.obj("error" -> "missing file")).as(JSON)))
      case _ => fuccess(BadRequest(Json.obj("error" -> "bad asset type")).as(JSON))

  private def indexPage(setup: Option[GameSetup], bots: JsArray, dev: Boolean)(using ctx: Context) =
    given setupFormat: Format[GameSetup] = Json.format[GameSetup]
    views.local.index(
      Json
        .obj("pref" -> pref, "bots" -> bots)
        .add("setup", setup)
        .add("assets", dev.option(env.local.api.assets)),
      if dev then "local.dev" else "local"
    )

  private def pref(using ctx: Context) =
    lila.pref.JsonView.write(ctx.pref, false).add("animationDuration", ctx.pref.animationMillis.some)

  private def optTrue(s: Option[String]) =
    s.exists(v => v == "" || v == "1" || v == "true")

  private def localOk(obj: JsObject) = JsonOk(obj).withHeaders("Service-Worker-Allowed" -> "/")
