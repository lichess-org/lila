package controllers

import play.api.libs.json._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.common.LightUser.lightUserWrites
import lidraughts.i18n.{ I18nKeys, I18nLangPicker, enLang }

object Dasher extends LidraughtsController {

  private def translations(implicit ctx: Context) = lidraughts.i18n.JsDump.keysToObject(
    ctx.isAnon.fold(
      List(
        I18nKeys.signIn,
        I18nKeys.signUp
      ),
      List(
        I18nKeys.profile,
        I18nKeys.inbox,
        I18nKeys.preferences,
        I18nKeys.logOut
      )
    ) ::: List(
        I18nKeys.networkLagBetweenYouAndLidraughts,
        I18nKeys.timeToProcessAMoveOnLidraughtsServer,
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
      ), lidraughts.i18n.I18nDb.Site, ctx.lang
  ) ++ lidraughts.i18n.JsDump.keysToObject(
      // the language settings should never be in a totally foreign language
      List(I18nKeys.language),
      lidraughts.i18n.I18nDb.Site,
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
              "list" -> lidraughts.pref.SoundSet.list.map { set =>
                s"${set.key} ${set.name}"
              }
            ),
            "background" -> Json.obj(
              "current" -> ctx.currentBg,
              "image" -> ctx.pref.bgImgOrDefault
            ),
            "board" -> Json.obj(
              "zoom" -> ctx.zoom
            ),
            "theme" -> Json.obj(
              "d2" -> Json.obj(
                "current" -> ctx.currentTheme.name,
                "list" -> lidraughts.pref.Theme.all.map(_.name)
              )
            ),
            "piece" -> Json.obj(
              "d2" -> Json.obj(
                "current" -> ctx.currentPieceSet.name,
                "list" -> lidraughts.pref.PieceSet.all.map(_.name)
              )
            ),
            "kid" -> ctx.me ?? (_.kid),
            "coach" -> isGranted(_.Coach),
            "streamer" -> isStreamer,
            "zen" -> ctx.pref.zen,
            "i18n" -> translations
          )
        }
      }
    )
  }
}
