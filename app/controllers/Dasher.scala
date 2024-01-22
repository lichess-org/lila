package controllers

import play.api.libs.json._

import lila.api.Context
import lila.app._
import lila.common.LightUser.lightUserWrites
import lila.i18n.{ enLang, I18nKeys => trans, I18nLangPicker, LangList }
import lila.pref.JsonView.customThemeWriter

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
    trans.standard,
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

  implicit private val pieceSetsJsonWriter = Writes[List[lila.pref.PieceSet]] { sets =>
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
