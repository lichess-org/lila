package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ given, * }
import lila.common.Json.given
import lila.local.{ AssetType, BotJson }

final class Local(env: Env) extends LilaController(env):

  extension (r: Result)
    def withServiceWorker(using RequestHeader) =
      r.enforceCrossSiteIsolation.withHeaders("Service-Worker-Allowed" -> "/")

  def index = Secure(_.Beta) { _ ?=> _ ?=>
    for
      bots <- env.local.repo.getLatestBots()
      res <- negotiate(
        html =
          for page <- renderPage(views.local.play(bots, prefJson))
          yield Ok(page).withServiceWorker,
        json = JsonOk(Json.obj("bots" -> bots))
      )
    yield res
  }

  def assetKeys = Anon: // for service worker
    JsonOk(env.local.api.getJson)

  def devIndex = Secure(_.BotEditor) { _ ?=> _ ?=>
    for
      bots   <- env.local.repo.getLatestBots()
      assets <- env.local.api.devGetAssets
      page   <- renderPage(views.local.dev(bots, prefJson, assets))
    yield Ok(page).withServiceWorker
  }

  def devAssets = Secure(_.BotEditor) { _ ?=> _ ?=>
    env.local.api.devGetAssets.map(JsonOk)
  }

  def devBotHistory(botId: Option[UserStr]) = Secure(_.BotEditor) { _ ?=> _ ?=>
    env.local.repo
      .getVersions(botId.map(_.id))
      .map: history =>
        JsonOk(Json.obj("bots" -> history))
  }

  def devPostBot = SecureBody(parse.json)(_.BotEditor) { ctx ?=> me ?=>
    ctx.body.body
      .validate[JsObject]
      .fold(
        err => BadRequest(jsonError(err.toString)),
        bot => env.local.repo.putBot(BotJson(bot), me.userId).map(JsonOk)
      )
  }

  def devNameAsset(key: String, name: String) = Secure(_.BotEditor): _ ?=>
    env.local.repo
      .nameAsset(none, key, name, none)
      .flatMap(_ => env.local.api.devGetAssets.map(JsonOk))

  def devDeleteAsset(key: String) = Secure(_.BotEditor): _ ?=>
    env.local.repo
      .deleteAsset(key)
      .flatMap(_ => env.local.api.devGetAssets.map(JsonOk))

  def devPostAsset(tpe: String, key: String) = SecureBody(parse.multipartFormData)(_.BotEditor) { ctx ?=>
    AssetType
      .read(tpe)
      .so: (tpe: AssetType) =>
        def formValue(field: String) = ctx.body.body.dataParts.get(field).flatMap(_.headOption)
        val author: Option[UserId]   = formValue("author").flatMap(UserStr.read).map(_.id)
        val name                     = formValue("name").getOrElse(key)
        ctx.body.body
          .file("file")
          .map: file =>
            env.local.api
              .storeAsset(tpe, key, file)
              .flatMap:
                case Left(error) => InternalServerError(jsonError(error)).as(JSON)
                case Right(assets) =>
                  for _ <- env.local.repo.nameAsset(tpe.some, key, name, author)
                  yield JsonOk(Json.obj("key" -> key, "name" -> name))
          .getOrElse(BadRequest(jsonError("missing file")).as(JSON))
  }

  private def prefJson(using ctx: Context) =
    lila.pref.JsonView
      .write(ctx.pref, false)
      .add("animationDuration", ctx.pref.animationMillis.some)
      .add("enablePremove", ctx.pref.premove.some)
      .add("showCaptured", ctx.pref.captured.some)
