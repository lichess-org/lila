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
import lila.security.Permission
import lila.local.{ GameSetup, AssetType }

final class Local(env: Env) extends LilaController(env):
  def index(
      white: Option[String],
      black: Option[String],
      fen: Option[String],
      time: Option[String],
      go: Option[String]
  ) = Open:
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
        JsonOk(Json.obj("bots" -> bots))

  def assetKeys = Open: // for service worker
    JsonOk(env.local.api.assetKeys)

  def devIndex = Auth: _ ?=>
    for
      bots   <- env.local.repo.getLatestBots()
      assets <- getDevAssets
      page   <- renderPage(indexPage(none, bots, assets.some))
    yield Ok(page).enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")

  def devAssets = Auth: ctx ?=>
    getDevAssets.map(JsonOk)

  def devBotHistory(botId: Option[String]) = Auth: _ ?=>
    env.local.repo
      .getVersions(botId.map(UserId.apply))
      .map: history =>
        JsonOk(Json.obj("bots" -> history))

  def devPostBot = SecureBody(parse.json)(_.BotEditor) { ctx ?=> me ?=>
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

  def devNameAsset(key: String, name: String) = Secure(_.BotEditor): _ ?=>
    env.local.repo
      .nameAsset(none, key, name, none)
      .flatMap(_ => getDevAssets.map(JsonOk))

  def devDeleteAsset(key: String) = Secure(_.BotEditor): _ ?=>
    env.local.repo
      .deleteAsset(key)
      .flatMap(_ => getDevAssets.map(JsonOk))

  def devPostAsset(notAString: String, key: String) = SecureBody(parse.multipartFormData)(_.BotEditor) {
    ctx ?=>
      val tpe: AssetType         = notAString.asInstanceOf[AssetType]
      val author: Option[String] = ctx.body.body.dataParts.get("author").flatMap(_.headOption)
      val name                   = ctx.body.body.dataParts.get("name").flatMap(_.headOption).getOrElse(key)
      ctx.body.body
        .file("file")
        .map: file =>
          env.local.api
            .storeAsset(tpe, key, file)
            .flatMap:
              case Left(error) => InternalServerError(Json.obj("error" -> error.toString)).as(JSON)
              case Right(assets) =>
                env.local.repo
                  .nameAsset(tpe.some, key, name, author)
                  .flatMap(_ => (JsonOk(Json.obj("key" -> key, "name" -> name))))
        .getOrElse(fuccess(BadRequest(Json.obj("error" -> "missing file")).as(JSON)))
  }

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
        .add("username", ctx.me.map(_.username))
        .add("canPost", isGrantedOpt(_.BotEditor)),
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
