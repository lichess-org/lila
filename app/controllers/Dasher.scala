package controllers

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.app.{ *, given }
import lila.common.Json.lightUserWrites
import lila.core.i18n.{ I18nKey as trans, defaultLang }
import lila.i18n.{ LangPicker, LangList }

final class Dasher(env: Env)(using ws: StandaloneWSClient) extends LilaController(env):

  private val translationsBase = List(
    trans.site.networkLagBetweenYouAndLichess,
    trans.site.timeToProcessAMoveOnLichessServer,
    trans.site.sound,
    trans.site.background,
    trans.site.light,
    trans.site.dark,
    trans.site.transparent,
    trans.site.deviceTheme,
    trans.site.backgroundImageUrl,
    trans.site.boardGeometry,
    trans.site.boardTheme,
    trans.site.boardSize,
    trans.site.pieceSet,
    trans.preferences.zenMode
  )

  private val translationsAnon = List(
    trans.site.signIn,
    trans.site.signUp
  ) ::: translationsBase

  private val translationsAuth = List(
    trans.site.profile,
    trans.site.inbox,
    trans.preferences.preferences,
    trans.site.coachManager,
    trans.site.streamerManager,
    trans.site.logOut
  ) ::: translationsBase

  private def translations(using ctx: Context) =
    val langLang =
      if LangPicker.allFromRequestHeaders(ctx.req).has(ctx.lang) then ctx.lang
      else LangPicker.bestFromRequestHeaders(ctx.req) | defaultLang
    lila.i18n.JsDump.keysToObject(
      if ctx.isAnon then translationsAnon else translationsAuth
    ) ++
      // the language settings should never be in a totally foreign language
      lila.i18n.JsDump.keysToObject(List(trans.site.language))(using ctx.translate.copy(lang = langLang))

  private lazy val galleryJson = env.memo.cacheApi.unit[Option[JsValue]]:
    _.refreshAfterWrite(1.minute)
      .buildAsyncFuture: _ =>
        ws.url(s"${env.net.assetBaseUrlInternal}/assets/lifat/background/gallery.json")
          .get()
          .map:
            case res if res.status == 200 => res.body[JsValue].some
            case _                        => none
          .recoverWith(_ => fuccess(none))

  def get = Open:
    negotiateJson:
      ctx.me
        .so(env.streamer.api.isPotentialStreamer(_))
        .zip(galleryJson.get({}))
        .map: (isStreamer, gallery) =>
          Ok:
            Json.obj(
              "user" -> ctx.me.map(_.light),
              "lang" -> Json.obj(
                "current"  -> ctx.lang.code,
                "accepted" -> LangPicker.allFromRequestHeaders(ctx.req).map(_.code),
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
                  "current" -> ctx.pref.currentTheme.name,
                  "list"    -> lila.pref.Theme.all.map(_.name)
                ),
                "d3" -> Json.obj(
                  "current" -> ctx.pref.currentTheme3d.name,
                  "list"    -> lila.pref.Theme3d.all.map(_.name)
                )
              ),
              "piece" -> Json.obj(
                "d2" -> Json.obj(
                  "current" -> ctx.pref.currentPieceSet.name,
                  "list"    -> lila.pref.PieceSet.all.map(_.name)
                ),
                "d3" -> Json.obj(
                  "current" -> ctx.pref.currentPieceSet3d.name,
                  "list"    -> lila.pref.PieceSet3d.all.map(_.name)
                )
              ),
              "coach"    -> isGrantedOpt(_.Coach),
              "streamer" -> isStreamer,
              "i18n"     -> translations
            )
