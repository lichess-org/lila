package controllers

import play.api.libs.json._

import lila.api.Context
import lila.app._
import lila.common.LightUser.lightUserWrites
import lila.i18n.{ enLang, I18nKeys => trans, I18nLangPicker, LangList }
import lila.pref.JsonView.CustomThemeWriter

final class Dasher(env: Env) extends LilaController(env) {

  private val translationsBase = List(
    trans.networkLagBetweenYouAndLishogi,
    trans.timeToProcessAMoveOnLishogiServer,
    trans.sound,
    trans.background,
    trans.light,
    trans.dark,
    trans.transparent,
    trans.customTheme,
    trans.backgroundImageUrl,
    trans.backgroundColor,
    trans.gridColor,
    trans.gridWidth,
    trans.none,
    trans.gridSlim,
    trans.gridThick,
    trans.gridVeryThick,
    trans.hands,
    trans.grid,
    trans.board,
    trans.boardTheme,
    trans.pieceSet,
    trans.default,
    trans.chushogi,
    trans.kyotoshogi,
    trans.preferences.zenMode,
    trans.notationSystem,
    trans.preferences.westernNotation,
    trans.preferences.japaneseNotation,
    trans.preferences.kitaoKawasakiNotation,
    trans.preferences.kifNotation,
    trans.insights.insights
  ).map(_.key)

  private val translationsAnon = List(
    trans.signIn,
    trans.signUp
  ).map(_.key) ::: translationsBase

  private val translationsAuth = List(
    trans.profile,
    trans.inbox,
    trans.streamerManager,
    trans.preferences.preferences,
    trans.coachManager,
    trans.logOut
  ).map(_.key) ::: translationsBase

  private def translations(implicit ctx: Context) =
    lila.i18n.JsDump.keysToObject(
      if (ctx.isAnon) translationsAnon else translationsAuth,
      ctx.lang
    ) ++ lila.i18n.JsDump.keysToObject(
      // the language settings should never be in a totally foreign language
      List(trans.language.key),
      if (I18nLangPicker.allFromRequestHeaders(ctx.req).has(ctx.lang)) ctx.lang
      else I18nLangPicker.bestFromRequestHeaders(ctx.req) | enLang
    )

  def get =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        api = _ =>
          ctx.me.??(env.streamer.api.isPotentialStreamer) map { isStreamer =>
            Ok {
              Json.obj(
                "user" -> ctx.me.map(_.light),
                "lang" -> Json.obj(
                  "current"  -> ctx.lang.code,
                  "accepted" -> I18nLangPicker.allFromRequestHeaders(ctx.req).map(_.code),
                  "list"     -> LangList.choices
                ),
                "sound" -> Json.obj(
                  "list" -> lila.pref.SoundSet.list.map { set =>
                    s"${set.key}|${set.name}"
                  }
                ),
                "background" -> Json.obj(
                  "current" -> ctx.currentBg,
                  "image"   -> ctx.pref.bgImgOrDefault
                ),
                "theme" -> Json.obj(
                  "thickGrid" -> ctx.pref.isUsingThickGrid,
                  "current"   -> ctx.currentTheme.name,
                  "list"      -> lila.pref.Theme.all.map(_.name)
                ),
                "customTheme" -> ctx.pref.customThemeOrDefault,
                "piece" -> Json.obj(
                  "current" -> ctx.currentPieceSet.name,
                  "list"    -> lila.pref.PieceSet.all.map(_.name)
                ),
                "chuPiece" -> Json.obj(
                  "current" -> ctx.currentChuPieceSet.name,
                  "list"    -> lila.pref.ChuPieceSet.all.map(_.name)
                ),
                "kyoPiece" -> Json.obj(
                  "current" -> ctx.currentKyoPieceSet.name,
                  "list"    -> lila.pref.KyoPieceSet.all.map(_.name)
                ),
                "inbox"    -> ctx.hasInbox,
                "coach"    -> isGranted(_.Coach),
                "streamer" -> isStreamer,
                "i18n"     -> translations,
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
