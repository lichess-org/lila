package controllers

import play.api.libs.json.*
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.JsonBodyReadables.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.LightUser.lightUserWrites
import lila.i18n.{ enLang, I18nKeys as trans, I18nLangPicker, LangList }

final class Dasher(env: Env)(using ws: StandaloneWSClient) extends LilaController(env):

  private val translationsBase = List(
    trans.networkLagBetweenYouAndLichess,
    trans.timeToProcessAMoveOnLichessServer,
    trans.sound,
    trans.background,
    trans.light,
    trans.dark,
    trans.transparent,
    trans.deviceTheme,
    trans.backgroundImageUrl,
    trans.boardGeometry,
    trans.boardTheme,
    trans.boardSize,
    trans.pieceSet,
    trans.preferences.zenMode
  )

  private val translationsAnon = List(
    trans.signIn,
    trans.signUp
  ) ::: translationsBase

  private val translationsAuth = List(
    trans.profile,
    trans.inbox,
    trans.preferences.preferences,
    trans.coachManager,
    trans.streamerManager,
    trans.logOut
  ) ::: translationsBase

  private def translations(using Context) =
    lila.i18n.JsDump.keysToObject(
      if (ctx.isAnon) translationsAnon else translationsAuth,
      ctx.lang
    ) ++ lila.i18n.JsDump.keysToObject(
      // the language settings should never be in a totally foreign language
      List(trans.language),
      if (I18nLangPicker.allFromRequestHeaders(ctx.req).has(ctx.lang)) ctx.lang
      else I18nLangPicker.bestFromRequestHeaders(ctx.req) | enLang
    )

  private lazy val galleryJson = env.memo.cacheApi.unit[Option[JsValue]]:
    _.refreshAfterWrite(1.minute)
      .buildAsyncFuture: _ =>
        ws.url(s"${env.net.assetBaseUrl}/assets/lifat/background/gallery.json")
          .get()
          .map:
            case res if res.status == 200 => res.body[JsValue].some
            case _                        => none

  def get = Open:
    negotiate(
      html = notFound,
      api = _ =>
        ctx.me
          .??(env.streamer.api.isPotentialStreamer)
          .zip(galleryJson.get({}))
          .map: (isStreamer, gallery) =>
            Ok:
              Json.obj(
                "user" -> ctx.me.map(_.light),
                "lang" -> Json.obj(
                  "current"  -> ctx.lang.code,
                  "accepted" -> I18nLangPicker.allFromRequestHeaders(ctx.req).map(_.code),
                  "list"     -> LangList.allChoices
                ),
                "sound" -> Json.obj(
                  "list" -> lila.pref.SoundSet.list.map { set =>
                    s"${set.key} ${set.name}"
                  }
                ),
                "background" -> Json
                  .obj(
                    "current" -> lila.pref.Pref.Bg.asString.get(ctx.pref.bg),
                    "image"   -> ctx.pref.bgImgOrDefault
                  )
                  .add("gallery", gallery),
                "board" -> Json.obj(
                  "is3d" -> ctx.pref.is3d
                ),
                "theme" -> Json.obj(
                  "d2" -> Json.obj(
                    "current" -> ctx.currentTheme.name,
                    "list"    -> lila.pref.Theme.all.map(_.name)
                  ),
                  "d3" -> Json.obj(
                    "current" -> ctx.currentTheme3d.name,
                    "list"    -> lila.pref.Theme3d.all.map(_.name)
                  )
                ),
                "piece" -> Json.obj(
                  "d2" -> Json.obj(
                    "current" -> ctx.currentPieceSet.name,
                    "list"    -> lila.pref.PieceSet.all.map(_.name)
                  ),
                  "d3" -> Json.obj(
                    "current" -> ctx.currentPieceSet3d.name,
                    "list"    -> lila.pref.PieceSet3d.all.map(_.name)
                  )
                ),
                "coach"    -> isGranted(_.Coach),
                "streamer" -> isStreamer,
                "i18n"     -> translations
              )
    )
