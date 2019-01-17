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
    if (ctx.blindMode) form3.hidden(form("move"), captcha.solutions.head.some)
    else {
      val url = netBaseUrl + routes.Round.watcher(captcha.gameId, if (captcha.white) "white" else "black")
      div(
        cls := List(
          "captcha form-group" -> true,
          "is-invalid" -> lidraughts.common.Captcha.isFailed(form)
        ),
        dataCheckUrl := routes.Main.captchaCheck(captcha.gameId)
      )(
          div(
            cls := "mini_board parse_fen is2d",
            dataPlayable := "1",
            dataX := encodeFen(safeJsonValue(Json.toJson(captcha.moves))),
            dataY := encodeFen(if (captcha.white) { "white" } else { "black" }),
            dataZ := encodeFen(captcha.fen)
          )(miniBoardContent),
          div(cls := "captcha-explanation")(
            label(cls := "form-label")(trans.colorPlaysCapture.frag(
              (if (captcha.white) trans.white else trans.black).frag()
            )),
            br, br,
            trans.thisIsADraughtsCaptcha.frag(),
            br,
            trans.clickOnTheBoardToMakeYourMove.frag(),
            br, br,
            trans.help.frag(),
            " ",
            a(cls := "hint--bottom", dataHint := trans.viewTheSolution.txt(), target := "_blank", href := url)(url),
            div(cls := "result success text", dataIcon := "E")(trans.success.frag()),
            div(cls := "result failure text", dataIcon := "k")(trans.notTheBestCapture.frag()),
            form3.hidden(form("move"))
          )
        )
    }
  )
}
