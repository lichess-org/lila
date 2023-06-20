package views.html.challenge

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge
import lila.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def js(c: Challenge, json: play.api.libs.json.JsObject, owner: Boolean, color: Option[shogi.Color] = None)(
      implicit ctx: Context
  ) =
    frag(
      jsTag("challenge.js", defer = true),
      embedJsUnsafe(s"""lishogi=window.lishogi||{};customWs=true;lishogi_challenge = ${safeJsonValue(
          Json.obj(
            "socketUrl" -> s"/challenge/${c.id}/socket/v$apiVersion",
            "xhrUrl"    -> routes.Challenge.show(c.id, color.map(_.name)).url,
            "owner"     -> owner,
            "data"      -> json
          )
        )}""")
    )

  def details(c: Challenge, mine: Boolean)(implicit ctx: Context) =
    div(cls := "details")(
      div(cls := "variant", dataIcon := (if (c.initialSfen.isDefined) '*' else c.perfType.iconChar))(
        div(
          views.html.game.bits.variantLink(c.variant, c.perfType.some),
          br,
          span(cls := "clock")(
            c.daysPerTurn map { days =>
              if (days == 1) trans.oneDay()
              else trans.nbDays.pluralSame(days)
            } getOrElse shortClockName(c.clock.map(_.config))
          )
        )
      ),
      div(cls := "game-color") {
        val handicap = c.initialSfen.fold(false)(sfen => shogi.Handicap.isHandicap(sfen, c.variant))
        frag(
          shogi.Color.fromName(c.colorChoice.toString.toLowerCase).fold(trans.randomColor.txt()) { color =>
            transWithColorName(trans.youPlayAsX, if (mine) color else !color, handicap)
          },
          " - ",
          transWithColorName(trans.xPlays, c.initialSfen.flatMap(_.color).getOrElse(shogi.Sente), handicap)
        )
      },
      div(cls := "mode")(modeName(c.mode))
    )
}
