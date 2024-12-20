package controllers

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import lila.app.{ *, given }
import lila.i18n.{ LangList, LangPicker }
import lila.pref.ui.DasherJson

final class Dasher(env: Env)(using ws: StandaloneWSClient) extends LilaController(env):

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
              "streamer" -> isStreamer
            ) ++ DasherJson(ctx.pref, gallery)
