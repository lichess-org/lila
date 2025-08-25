package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ given, * }
import lila.common.Json.given
import lila.jsBot.{ AssetType, BotJson }

final class JsBot(env: Env) extends LilaController(env):

  def index = Secure(_.Beta) { _ ?=> _ ?=>
    for
      bots <- env.jsBot.repo.getLatestBots()
      res <- negotiate(
        html =
          for page <- renderPage(views.jsBot.play(bots, prefJson))
          yield Ok(page).withServiceWorker,
        json = JsonOk(Json.obj("bots" -> bots))
      )
    yield res
  }

  def assetKeys = Anon: // for service worker
    JsonOk(env.jsBot.api.getJson)

  def devIndex = Secure(_.BotEditor) { _ ?=> _ ?=>
    for
      bots <- env.jsBot.repo.getLatestBots()
      assets <- env.jsBot.api.devGetAssets
      page <- renderPage(views.jsBot.dev(bots, prefJson, assets))
    yield Ok(page).withServiceWorker
  }

  def devAssets = Secure(_.BotEditor) { _ ?=> _ ?=>
    env.jsBot.api.devGetAssets.map(JsonOk)
  }

  def devBotHistory(botId: Option[UserStr]) = Secure(_.BotEditor) { _ ?=> _ ?=>
    env.jsBot.repo
      .getVersions(botId.map(_.id))
      .map: history =>
        JsonOk(Json.obj("bots" -> history))
  }

  def devPostBot = SecureBody(parse.json)(_.BotEditor) { ctx ?=> me ?=>
    ctx.body.body
      .validate[JsObject]
      .fold(
        err => BadRequest(jsonError(err.toString)),
        bot => env.jsBot.repo.putBot(BotJson(bot), me.userId).map(JsonOk)
      )
  }

  def devNameAsset(key: String, name: String) = Secure(_.BotEditor): _ ?=>
    env.jsBot.repo
      .nameAsset(none, key, name, none)
      .flatMap(_ => env.jsBot.api.devGetAssets.map(JsonOk))

  def devDeleteAsset(key: String) = Secure(_.BotEditor): _ ?=>
    env.jsBot.repo
      .deleteAsset(key)
      .flatMap(_ => env.jsBot.api.devGetAssets.map(JsonOk))

  def devPostAsset(tpe: String, key: String) = SecureBody(parse.multipartFormData)(_.BotEditor) { ctx ?=>
    AssetType
      .read(tpe)
      .so: (tpe: AssetType) =>
        def formValue(field: String) = ctx.body.body.dataParts.get(field).flatMap(_.headOption)
        val author: Option[UserId] = formValue("author").flatMap(UserStr.read).map(_.id)
        val name = formValue("name").getOrElse(key)
        ctx.body.body
          .file("file")
          .map: file =>
            env.jsBot.api
              .storeAsset(tpe, key, file)
              .flatMap:
                case Left(error) => InternalServerError(jsonError(error)).as(JSON)
                case Right(_) =>
                  for _ <- env.jsBot.repo.nameAsset(tpe.some, key, name, author)
                  yield JsonOk(Json.obj("key" -> key, "name" -> name))
          .getOrElse(BadRequest(jsonError("missing file")).as(JSON))
  }

  // def test = Open:
  //   renderPage(views.jsBot.roundPlay(prefJson)).flatMap(Ok(_).withServiceWorker)

  private def prefJson(using ctx: Context) =
    lila.pref
      .toJson(ctx.pref, false)
      .add("animationDuration", ctx.pref.animationMillis.some)
      .add("enablePremove", ctx.pref.premove.some)
      .add("showCaptured", ctx.pref.captured.some)
