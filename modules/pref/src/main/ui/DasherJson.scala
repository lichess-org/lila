package lila.pref
package ui

import play.api.libs.json.*

import lila.ui.Context
import lila.core.i18n.{ I18nKey as trans }
import lila.common.Json.given
import lila.core.perm.Granter

object DasherJson:

  object i18n:
    val base = List(
      trans.site.networkLagBetweenYouAndLichess,
      trans.site.timeToProcessAMoveOnLichessServer,
      trans.site.sound,
      trans.site.background,
      trans.site.light,
      trans.site.dark,
      trans.site.transparent,
      trans.site.deviceTheme,
      trans.site.backgroundImageUrl,
      trans.site.board,
      trans.site.pieceSet,
      trans.preferences.zenMode
    )

    val anon = List(
      trans.site.signIn,
      trans.site.signUp
    ) ::: base

    val auth = List(
      trans.site.profile,
      trans.site.inbox,
      trans.preferences.preferences,
      trans.site.coachManager,
      trans.site.streamerManager,
      trans.site.logOut
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
