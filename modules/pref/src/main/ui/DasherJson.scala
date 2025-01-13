package lila.pref
package ui

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.i18n.I18nKey as trans
import lila.core.perm.Granter
import lila.ui.Context

object DasherJson:

  object i18n:
    import trans.site as trs
    val base = List(
      trs.networkLagBetweenYouAndLichess,
      trs.timeToProcessAMoveOnLichessServer,
      trs.sound,
      trs.background,
      trs.light,
      trs.dark,
      trs.transparent,
      trs.deviceTheme,
      trs.backgroundImageUrl,
      trs.board,
      trs.size,
      trs.opacity,
      trs.brightness,
      trs.hue,
      trs.boardReset,
      trs.pieceSet,
      trans.preferences.zenMode
    )

    val anon = List(
      trs.signIn,
      trs.signUp
    ) ::: base

    val auth = List(
      trs.profile,
      trs.inbox,
      trans.preferences.preferences,
      trs.coachManager,
      trs.streamerManager,
      trs.logOut
    ) ::: base

  def apply(pref: Pref, gallery: Option[JsValue])(using ctx: Context): JsObject =
    Json.obj(
      "user" -> ctx.me.map(_.light),
      "sound" -> Json.obj(
        "list" -> SoundSet.list.map { set =>
          s"${set.key} ${set.name}"
        }
      ),
      "background" -> Json
        .obj(
          "current" -> Pref.Bg.asString.get(pref.bg),
          "image"   -> pref.bgImgOrDefault
        )
        .add("gallery", gallery),
      "board" -> Json.obj(
        "is3d" -> pref.is3d,
        "d2" -> Json.obj(
          "current" -> pref.currentTheme.name,
          "list"    -> Theme.all.map(_.name)
        ),
        "d3" -> Json.obj(
          "current" -> pref.currentTheme3d.name,
          "list"    -> Theme3d.all.map(_.name)
        )
      ),
      "piece" -> Json.obj(
        "d2" -> Json.obj(
          "current" -> pref.currentPieceSet.name,
          "list"    -> PieceSet.all.map(_.name)
        ),
        "d3" -> Json.obj(
          "current" -> pref.currentPieceSet3d.name,
          "list"    -> PieceSet3d.all.map(_.name)
        )
      ),
      "coach" -> Granter.opt(_.Coach)(using ctx.me)
    )
