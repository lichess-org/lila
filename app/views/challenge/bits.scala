package views.html.challenge

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge
import lila.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def js(c: Challenge, json: play.api.libs.json.JsObject, owner: Boolean, color: Option[chess.Color] = None)(
      implicit ctx: Context
  ) =
    frag(
      jsModule("challengePage"),
      embedJsUnsafeLoadThen(s"""challengePageStart(${safeJsonValue(
        Json.obj(
          "socketUrl" -> s"/challenge/${c.id}/socket/v$apiVersion",
          "xhrUrl"    -> routes.Challenge.show(c.id, color.map(_.name)).url,
          "owner"     -> owner,
          "data"      -> json
        )
      )})""")
    )

  def details(c: Challenge)(implicit ctx: Context) =
    div(cls := "details")(
      div(cls := "variant", dataIcon := (if (c.initialFen.isDefined) '' else c.perfType.iconChar))(
        div(
          views.html.game.bits.variantLink(c.variant, c.perfType.some, c.initialFen),
          br,
          span(cls := "clock")(
            c.daysPerTurn map { days =>
              if (days == 1) trans.oneDay()
              else trans.nbDays.pluralSame(days)
            } getOrElse shortClockName(c.clock.map(_.config))
          )
        )
      ),
      div(cls := "mode")(
        s"${c.colorChoice.toString()} • ${modeName(c.mode)}"
      )
    )
}
