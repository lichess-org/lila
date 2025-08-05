package controllers

import java.nio.file.Files
import play.api.libs.json.*
import lila.app.{ *, given }
import lila.i18n.{ LangList, LangPicker }
import lila.pref.ui.DasherJson

final class Dasher(env: Env) extends LilaController(env):

  private lazy val galleryJson = scalalib.data.SimpleMemo[Option[JsValue]](10.minutes.some): () =>
    val pathname = env.getFile.exec(s"public/lifat/background/gallery.json").toPath
    try Json.parse(Files.newInputStream(pathname)).some
    catch
      case e: Throwable =>
        lila.log("dasher").warn(s"Error reading gallery json $pathname", e)
        none

  def get = Open:
    negotiateJson:
      ctx.me
        .so(env.streamer.api.isPotentialStreamer(_))
        .map: isStreamer =>
          val gallery = galleryJson.get()
          Ok:
            Json.obj(
              "lang" -> Json.obj(
                "current" -> ctx.lang.code,
                "accepted" -> LangPicker.allFromRequestHeaders(ctx.req).map(_.code),
                "list" -> LangList.allChoices
              ),
              "streamer" -> isStreamer
            ) ++ DasherJson(ctx.pref, gallery)
