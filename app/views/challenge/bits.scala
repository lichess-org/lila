package views.html.challenge

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object bits {

  def js(c: lila.challenge.Challenge, json: play.api.libs.json.JsObject, owner: Boolean)(implicit ctx: Context) =
    frag(
      jsTag("challenge.js", async = true),
      embedJs(s"""lichess=window.lichess||{};customWs=true;lichess_challenge = {
socketUrl: '${routes.Challenge.websocket(c.id, apiVersion.value)}',
xhrUrl: '${routes.Challenge.show(c.id)}',
owner: $owner,
data: ${safeJsonValue(json)}
};""")
    )

  def explanation(c: lila.challenge.Challenge)(implicit ctx: Context) = p(
    views.html.game.bits.variantLink(c.variant, variantName(c.variant)),
    " • ",
    modeName(c.mode),
    br,
    c.daysPerTurn map { days =>
      span(cls := "text", dataIcon := ";")(
        if (days == 1) trans.oneDay.frag()
        else trans.nbDays.pluralSameFrag(days)
      )
    } getOrElse span(cls := "text", dataIcon := "p")(shortClockName(c.clock.map(_.config)))
  )
}
