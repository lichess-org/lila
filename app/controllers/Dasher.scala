package controllers

import play.api.libs.json._

import lila.app._
import lila.common.LightUser.lightUserWrites
import lila.i18n.{ I18nLangPicker, LangList }
import lila.pref.JsonView.customThemeWriter
import lila.pref.PieceSet

final class Dasher(env: Env) extends LilaController(env) {

  implicit private val pieceSetsJsonWriter: Writes[List[PieceSet]] = Writes[List[lila.pref.PieceSet]] { sets =>
    JsArray(sets.map { set =>
      Json.obj(
        "key"  -> set.key,
        "name" -> set.name
      )
    })
  }

  def get =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        json = ctx.me.??(env.streamer.api.isPotentialStreamer) map { isStreamer =>
          Ok {
            Json.obj(
              "user" -> ctx.me.map(_.light),
              "lang" -> Json.obj(
                "current"  -> ctx.lang.code,
                "accepted" -> I18nLangPicker.allFromRequestHeaders(ctx.req).map(_.code),
                "list"     -> LangList.choices
              ),
              "sound" -> Json.obj(
                "list" -> JsArray(lila.pref.SoundSet.list.map { sound =>
                  Json.obj(
                    "key"  -> sound.key,
                    "name" -> sound.name
                  )
                })
              ),
              "background" -> Json.obj(
                "current" -> ctx.currentBg,
                "image"   -> ctx.pref.bgImgOrDefault
              ),
              "theme" -> Json.obj(
                "thickGrid" -> ctx.pref.isUsingThickGrid,
                "current"   -> ctx.currentTheme.key,
                "list" -> JsArray(lila.pref.Theme.all.map { theme =>
                  Json.obj(
                    "key"  -> theme.key,
                    "name" -> theme.name
                  )
                })
              ),
              "customTheme" -> ctx.pref.customThemeOrDefault,
              "piece" -> Json.obj(
                "current" -> ctx.currentPieceSet.key,
                "list"    -> lila.pref.PieceSet.all
              ),
              "chuPiece" -> Json.obj(
                "current" -> ctx.currentChuPieceSet.key,
                "list"    -> lila.pref.ChuPieceSet.all
              ),
              "kyoPiece" -> Json.obj(
                "current" -> ctx.currentKyoPieceSet.key,
                "list"    -> lila.pref.KyoPieceSet.all
              ),
              "inbox"    -> ctx.hasInbox,
              "coach"    -> isGranted(_.Coach),
              "streamer" -> isStreamer,
              "notation" -> Json.obj(
                "current" -> ctx.pref.notation,
                "list"    -> lila.pref.Notations.all.map(_.index)
              )
            )
          }
        }
      )
    }

}
