package views.html.challenge

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.challenge.Challenge
import lila.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def js(c: Challenge, json: play.api.libs.json.JsObject, owner: Boolean)(implicit ctx: Context) =
    frag(
      jsTag("challenge.js", defer = true),
      embedJsUnsafe(s"""lichess=window.lichess||{};customWs=true;lichess_challenge = ${
        safeJsonValue(Json.obj(
          "socketUrl" -> routes.Challenge.websocket(c.id, apiVersion.value).url,
          "xhrUrl" -> routes.Challenge.show(c.id).url,
          "owner" -> owner,
          "data" -> json
        ))
      }""")
    )

  def details(c: Challenge)(implicit ctx: Context) = div(cls := "details")(
    div(cls := "variant", dataIcon := (if (c.initialFen.isDefined) '*' else c.perfType.iconChar))(
      div(
        if (c.variant.exotic)
          views.html.game.bits.variantLink(c.variant, variantName(c.variant))
        else
          c.perfType.name,
        br,
        span(cls := "clock")(
          c.daysPerTurn map { days =>
            if (days == 1) trans.oneDay()
            else trans.nbDays.pluralSame(days)
          } getOrElse shortClockName(c.clock.map(_.config))
        )
      )
    ),
    div(cls := "mode")(modeName(c.mode))
  )
}
