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
      bots <- env.local.repo.getLatestBots()
      page <- renderPage(indexPage(setup, bots, none))
    yield Ok(page).enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")

  def bots = Open:
    env.local.repo
      .getLatestBots()
      .map: bots =>
        JsonOk(Json.obj("bots" -> bots.pp))

  def assetKeys = Open: // for service worker
    JsonOk(env.local.api.assetKeys)

  def devIndex = AuthBody: _ ?=>
    for
      bots   <- env.local.repo.getLatestBots()
      assets <- getDevAssets
      page   <- renderPage(indexPage(none, bots, assets.some))
    yield Ok(page).enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")

  def devBotHistory = AuthBody: _ ?=>
    env.local.repo
      .getAllBots()
      .map: history =>
        JsonOk(Json.obj("bots" -> history))

  def devPostBot = AuthBody(parse.json) { ctx ?=> me ?=>
    ctx.body.body
      .validate[JsObject]
      .fold(
        err => BadRequest(Json.obj("error" -> err.toString)),
        bot =>
          env.local.repo
            .putBot(bot, me.userId)
            .map: updatedBot =>
              JsonOk(updatedBot)
      )
  }

  def devNameAsset(key: String, name: String) = AuthBody: _ ?=>
    env.local.repo
      .nameAsset(key, name)
      .flatMap(_ => getDevAssets.map(JsonOk))

  def devDeleteAsset(key: String) = AuthBody: _ ?=>
    env.local.repo
      .deleteAsset(key)
      .flatMap(_ => getDevAssets.map(JsonOk))

  def devAssets = AuthBody: ctx ?=>
    getDevAssets.map(JsonOk)

  def devPostAsset(tpe: String, key: String, name: String) = Action.async(parse.multipartFormData): request =>
    val assetType: Option[AssetType] = tpe match
      case "image" => "image".some
      case "book"  => "book".some
      case "sound" => "sound".some
      case _       => none
    assetType match
      case Some(tpe) =>
        request.body
          .file("file")
          .map: file =>
            env.local.api
              .storeAsset(tpe, key, file)
              .flatMap:
                case Left(error) => InternalServerError(Json.obj("error" -> error.toString)).as(JSON)
                case Right(assets) =>
                  env.local.repo
                    .nameAsset(key, name)
                    .flatMap(_ => (JsonOk(Json.obj("key" -> key, "name" -> name))))
          .getOrElse(fuccess(BadRequest(Json.obj("error" -> "missing file")).as(JSON)))
      case _ => fuccess(BadRequest(Json.obj("error" -> "bad asset type")).as(JSON))

  private def indexPage(setup: Option[GameSetup], bots: JsArray, devAssets: Option[JsObject] = none)(using
      ctx: Context
  ) =
    given setupFormat: Format[GameSetup] = Json.format[GameSetup]
    views.local.index(
      Json
        .obj("pref" -> pref, "bots" -> bots)
        .add("setup", setup)
        .add("assets", devAssets)
        .add("userId", ctx.me.map(_.userId))
        .add("username", ctx.me.map(_.username)),
      if devAssets.isDefined then "local.dev" else "local"
    )

  private def getDevAssets =
    env.local.repo.getAssets.map: m =>
      JsObject:
        env.local.api.assetKeys
          .as[JsObject]
          .fields
          .collect:
            case (category, JsArray(keys)) =>
              category -> JsArray:
                keys.collect:
                  case JsString(key) if m.contains(key) =>
                    Json.obj("key" -> key, "name" -> m(key))

  private def pref(using ctx: Context) =
    lila.pref.JsonView
      .write(ctx.pref, false)
      .add("animationDuration", ctx.pref.animationMillis.some)
      .add("enablePremove", ctx.pref.premove.some)

  private def optTrue(s: Option[String]) =
    s.exists(v => v == "" || v == "1" || v == "true")
