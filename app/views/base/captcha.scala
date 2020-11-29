package views.html.base

import lila.common.String.html.safeJsonValue
import play.api.libs.json.Json
import scala.language.reflectiveCalls

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object captcha {

  private val dataCheckUrl = attr("data-check-url")
  private val dataPlayable = attr("data-playable")
  private val dataX        = attr("data-x")
  private val dataY        = attr("data-y")
  private val dataZ        = attr("data-z")

  def apply(form: lila.common.Form.FormLike, captcha: lila.common.Captcha)(implicit ctx: Context) =
    frag(
      form3.hidden(form("gameId"), captcha.gameId.some),
      if (ctx.blind) form3.hidden(form("move"), captcha.solutions.head.some)
      else {
        val url = netBaseUrl + routes.Round.watcher(captcha.gameId, if (captcha.white) "white" else "black")
        div(
          cls := List(
            "captcha form-group" -> true,
            "is-invalid"         -> lila.common.Captcha.isFailed(form)
          ),
          dataCheckUrl := routes.Main.captchaCheck(captcha.gameId)
        )(
          div(cls := "challenge")(
            div(
              cls := "mini-board cg-wrap parse-fen is2d",
              dataPlayable := "1",
              dataX := encodeFen(safeJsonValue(Json.toJson(captcha.moves))),
              dataY := encodeFen(if (captcha.white) {
                "white"
              } else {
                "black"
              }),
              dataZ := encodeFen(captcha.fen)
            )(cgWrapContent)
          ),
          div(cls := "captcha-explanation")(
            label(cls := "form-label")(
              if (captcha.white) trans.blackCheckmatesInOneMove() // swapped
              else trans.whiteCheckmatesInOneMove()
            ),
            br,
            br,
            trans.thisIsAChessCaptcha(),
            br,
            trans.clickOnTheBoardToMakeYourMove(),
            br,
            br,
            trans.help(),
            " ",
            a(title := trans.viewTheSolution.txt(), target := "_blank", href := url)(url),
            div(cls := "result success text", dataIcon := "E")(trans.checkmate()),
            div(cls := "result failure text", dataIcon := "k")(trans.notACheckmate()),
            form3.hidden(form("move"))
          )
        )
      }
    )
}
