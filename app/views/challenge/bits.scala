package views.html.challenge

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.challenge.Challenge
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def js(c: Challenge, json: play.api.libs.json.JsObject, owner: Boolean)(implicit ctx: Context) =
    frag(
      jsTag("challenge.js", async = true),
      embedJs(s"""lidraughts=window.lidraughts||{};customWs=true;lidraughts_challenge = {
socketUrl: '${routes.Challenge.websocket(c.id, apiVersion.value)}',
xhrUrl: '${routes.Challenge.show(c.id)}',
owner: $owner,
data: ${safeJsonValue(json)}
};""")
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
            if (days == 1) trans.oneDay.frag()
            else trans.nbDays.pluralSameFrag(days)
          } getOrElse shortClockName(c.clock.map(_.config))
        )
      )
    ),
    div(cls := "mode")(modeName(c.mode))
  )
}
