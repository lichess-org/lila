package controllers

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.app.{ *, given }
import lila.core.i18n.{ I18nKey as trans, defaultLang }
import lila.i18n.{ LangList, LangPicker }
import lila.pref.ui.DasherJson

final class Dasher(env: Env)(using ws: StandaloneWSClient) extends LilaController(env):

  private def translations(using ctx: Context) =
    val langLang =
      if LangPicker.allFromRequestHeaders(ctx.req).has(ctx.lang) then ctx.lang
      else LangPicker.bestFromRequestHeaders(ctx.req) | defaultLang
    lila.i18n.JsDump.keysToObject(
      if ctx.isAnon then DasherJson.i18n.anon else DasherJson.i18n.auth
    ) ++
      // the language settings should never be in a totally foreign language
      lila.i18n.JsDump.keysToObject(List(trans.site.language))(using ctx.translate.copy(lang = langLang))

  private lazy val galleryJson = env.memo.cacheApi.unit[Option[JsValue]]:
    _.refreshAfterWrite(10.minutes).buildAsyncFuture: _ =>
      ws.url(s"${env.net.assetBaseUrlInternal}/assets/lifat/background/gallery.json")
        .get()
        .map:
          case res if res.status == 200 => res.body[JsValue].some
          case _                        => none
        .recoverDefault

  def get = Open:
    negotiateJson:
      ctx.me
        .so(env.streamer.api.isPotentialStreamer(_))
        .zip(galleryJson.get({}))
        .map: (isStreamer, gallery) =>
          Ok:
            Json.obj(
              "lang" -> Json.obj(
                "current"  -> ctx.lang.code,
                "accepted" -> LangPicker.allFromRequestHeaders(ctx.req).map(_.code),
                "list"     -> LangList.allChoices
              ),
              "streamer" -> isStreamer,
              "i18n"     -> translations
            ) ++ DasherJson(ctx.pref, gallery)
