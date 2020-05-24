package views.html.base

import lidraughts.common.String.html.safeJsonValue
import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object captcha {

  private val dataCheckUrl = attr("data-check-url")
  private val dataPlayable = attr("data-playable")
  private val dataX = attr("data-x")
  private val dataY = attr("data-y")
  private val dataZ = attr("data-z")

  def apply(form: lidraughts.common.Form.FormLike, captcha: lidraughts.common.Captcha)(implicit ctx: Context) = frag(
    form3.hidden(form("gameId"), captcha.gameId.some),
    if (ctx.blind) form3.hidden(form("move"), captcha.solutions.head.some)
    else {
      val url = netBaseUrl + routes.Round.watcher(captcha.gameId, if (captcha.white) "white" else "black")
      div(
        cls := List(
          "captcha form-group" -> true,
          "is-invalid" -> lidraughts.common.Captcha.isFailed(form)
        ),
        dataCheckUrl := routes.Main.captchaCheck(captcha.gameId)
      )(
          div(cls := "challenge")(
            div(
              cls := "mini-board cg-wrap parse-fen is2d is100",
              dataPlayable := "1",
              dataX := encodeFen(safeJsonValue(Json.toJson(captcha.moves))),
              dataY := encodeFen(if (captcha.white) { "white" } else { "black" }),
              dataZ := encodeFen(captcha.fen)
            )(cgWrapContent)
          ),
          div(cls := "captcha-explanation")(
            label(cls := "form-label")(trans.colorPlaysCapture(
              if (captcha.white) trans.white.txt() else trans.black.txt()
            )),
            br, br,
            trans.thisIsADraughtsCaptcha(),
            br,
            trans.clickOnTheBoardToMakeYourMove(),
            br, br,
            trans.help(),
            ": ",
            a(title := trans.viewTheSolution.txt(), target := "_blank", href := url)(url),
            div(cls := "result success text", dataIcon := "E")(trans.success()),
            div(cls := "result failure text", dataIcon := "k")(trans.notTheBestCapture()),
            form3.hidden(form("move"))
          )
        )
    }
  )
}
