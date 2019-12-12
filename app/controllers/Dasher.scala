package controllers

import play.api.libs.json._

import lila.api.Context
import lila.app._
import lila.common.LightUser.lightUserWrites
import lila.i18n.{ I18nKeys, I18nLangPicker, enLang }

object Dasher extends LilaController {

  private val translationsBase = List(
    I18nKeys.networkLagBetweenYouAndLichess,
    I18nKeys.timeToProcessAMoveOnLichessServer,
    I18nKeys.sound,
    I18nKeys.background,
    I18nKeys.light,
    I18nKeys.dark,
    I18nKeys.transparent,
    I18nKeys.backgroundImageUrl,
    I18nKeys.boardGeometry,
    I18nKeys.boardTheme,
    I18nKeys.boardSize,
    I18nKeys.pieceSet,
    I18nKeys.zenMode
  )

  private val translationsAnon = List(
    I18nKeys.signIn,
    I18nKeys.signUp
  ) ::: translationsBase

  private val translationsAuth = List(
    I18nKeys.profile,
    I18nKeys.inbox,
    I18nKeys.preferences,
    I18nKeys.logOut
  ) ::: translationsBase

  private def translations(implicit ctx: Context) = lila.i18n.JsDump.keysToObject(
    if (ctx.isAnon) translationsAnon else translationsAuth,
    lila.i18n.I18nDb.Site,
    ctx.lang
  ) ++ lila.i18n.JsDump.keysToObject(
      // the language settings should never be in a totally foreign language
      List(I18nKeys.language),
      lila.i18n.I18nDb.Site,
      if (I18nLangPicker.allFromRequestHeaders(ctx.req).has(ctx.lang)) ctx.lang
      else I18nLangPicker.bestFromRequestHeaders(ctx.req) | enLang
    )

  def get = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => ctx.me.??(Env.streamer.api.isStreamer) map { isStreamer =>
        Ok {
          Json.obj(
            "user" -> ctx.me.map(_.light),
            "lang" -> Json.obj(
              "current" -> ctx.lang.code,
              "accepted" -> I18nLangPicker.allFromRequestHeaders(ctx.req).map(_.code)
            ),
            "sound" -> Json.obj(
              "list" -> lila.pref.SoundSet.list.map { set =>
                s"${set.key} ${set.name}"
              }
            ),
            "background" -> Json.obj(
              "current" -> ctx.currentBg,
              "image" -> ctx.pref.bgImgOrDefault
            ),
            "board" -> Json.obj(
              "is3d" -> ctx.pref.is3d
            ),
            "theme" -> Json.obj(
              "d2" -> Json.obj(
                "current" -> ctx.currentTheme.name,
                "list" -> lila.pref.Theme.all.map(_.name)
              ),
              "d3" -> Json.obj(
                "current" -> ctx.currentTheme3d.name,
                "list" -> lila.pref.Theme3d.all.map(_.name)
              )
            ),
            "piece" -> Json.obj(
              "d2" -> Json.obj(
                "current" -> ctx.currentPieceSet.name,
                "list" -> lila.pref.PieceSet.all.map(_.name)
              ),
              "d3" -> Json.obj(
                "current" -> ctx.currentPieceSet3d.name,
                "list" -> lila.pref.PieceSet3d.all.map(_.name)
              )
            ),
            "kid" -> ctx.me ?? (_.kid),
            "coach" -> isGranted(_.Coach),
            "streamer" -> isStreamer,
            "i18n" -> translations
          )
        }
      }
    )
  }
}
