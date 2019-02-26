package views.html.base

import lila.common.String.html.safeJsonValue
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object captcha {

  private val dataCheckUrl = attr("data-check-url")
  private val dataPlayable = attr("data-playable")
  private val dataX = attr("data-x")
  private val dataY = attr("data-y")
  private val dataZ = attr("data-z")

  def apply(form: lila.common.Form.FormLike, captcha: lila.common.Captcha)(implicit ctx: Context) = frag(
    form3.hidden(form("gameId"), captcha.gameId.some),
    if (ctx.blind) form3.hidden(form("move"), captcha.solutions.head.some)
    else {
      val url = netBaseUrl + routes.Round.watcher(captcha.gameId, if (captcha.white) "white" else "black")
      div(
        cls := List(
          "captcha form-group" -> true,
          "is-invalid" -> lila.common.Captcha.isFailed(form)
        ),
        dataCheckUrl := routes.Main.captchaCheck(captcha.gameId)
      )(
          div(
            cls := "mini-board cg-board-wrap parse-fen is2d",
            dataPlayable := "1",
            dataX := encodeFen(safeJsonValue(Json.toJson(captcha.moves))),
            dataY := encodeFen(if (captcha.white) { "white" } else { "black" }),
            dataZ := encodeFen(captcha.fen)
          )(div(cls := "cg-board")),
          div(cls := "captcha-explanation")(
            label(cls := "form-label")(trans.colorPlaysCheckmateInOne.frag(
              (if (captcha.white) trans.white else trans.black).frag()
            )),
            br, br,
            trans.thisIsAChessCaptcha.frag(),
            br,
            trans.clickOnTheBoardToMakeYourMove.frag(),
            br, br,
            trans.help.frag(),
            " ",
            a(title := trans.viewTheSolution.txt(), target := "_blank", href := url)(url),
            div(cls := "result success text", dataIcon := "E")(trans.checkmate.frag()),
            div(cls := "result failure text", dataIcon := "k")(trans.notACheckmate.frag()),
            form3.hidden(form("move"))
          )
        )
    }
  )
}
