package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ given, * }
import lila.common.Json.given
import lila.jsBot.{ BotUid, AssetType, BotJson }

final class JsBot(env: Env) extends LilaController(env):

  def index = Beta:
    for
      bots <- ctx.useMe(env.jsBot.api.playable.get(env.team.isBetaTester))
      res <-
        if bots.isEmpty then notFound
        else
          negotiate(
            html =
              for page <- renderPage(views.jsBot.play(bots, prefJson))
              yield Ok(page).withServiceWorker,
            json = JsonOk(Json.obj("bots" -> bots))
          )
    yield res

  def assetKeys = Anon: // for service worker
    JsonOk(env.jsBot.assets.getJson)

  def devIndex = Secure(_.BotEditor) { _ ?=> _ ?=>
    for
      bots <- env.jsBot.repo.getLatestBots()
      assets <- env.jsBot.assets.devGetAssets
      page <- renderPage(views.jsBot.dev(bots, prefJson, assets))
    yield Ok(page).withServiceWorker
  }

  def devAssets = Secure(_.BotEditor) { _ ?=> _ ?=>
    env.jsBot.assets.devGetAssets.map(JsonOk)
  }

  def devBotHistory(botId: Option[String]) = Secure(_.BotEditor) { _ ?=> _ ?=>
    env.jsBot.repo
      .getVersions(BotUid.from(botId))
      .map: history =>
        JsonOk(Json.obj("bots" -> history))
  }

  def devPostBot = SecureBody(parse.json)(_.BotEditor) { ctx ?=> me ?=>
    ctx.body.body
      .validate[JsObject]
      .fold(
        err => BadRequest(jsonError(err.toString)),
        bot => JsonOk(env.jsBot.api.put(BotJson(bot)))
      )
  }

  def devNameAsset(key: String, name: String) = Secure(_.BotEditor): _ ?=>
    env.jsBot.repo
      .nameAsset(none, key, name, none)
      .flatMap(_ => env.jsBot.assets.devGetAssets.map(JsonOk))

  def devDeleteAsset(key: String) = Secure(_.BotEditor): _ ?=>
    env.jsBot.repo
      .deleteAsset(key)
      .flatMap(_ => env.jsBot.assets.devGetAssets.map(JsonOk))

  def devPostAsset(tpe: String, key: String) = SecureBody(parse.multipartFormData)(_.BotEditor) { ctx ?=>
    AssetType
      .read(tpe)
      .so: (tpe: AssetType) =>
        def formValue(field: String) = ctx.body.body.dataParts.get(field).flatMap(_.headOption)
        val author: Option[UserId] = formValue("author").flatMap(UserStr.read).map(_.id)
        val name = formValue("name").getOrElse(key)
        ctx.body.body
          .file("file")
          .fold(BadRequest(jsonError("missing file")).as(JSON).toFuccess): file =>
            for
              _ <- env.jsBot.assets.storeAsset(tpe, key, file)
              _ <- env.jsBot.repo.nameAsset(tpe.some, key, name, author)
            yield JsonOk(Json.obj("key" -> key, "name" -> name))
  }

  // def test = Open:
  //   renderPage(views.jsBot.roundPlay(prefJson)).flatMap(Ok(_).withServiceWorker)

  private def prefJson(using ctx: Context) =
    lila.pref
      .toJson(ctx.pref, false)
      .add("animationDuration", ctx.pref.animationMillis.some)
      .add("enablePremove", ctx.pref.premove.some)
      .add("showCaptured", ctx.pref.captured.some)
