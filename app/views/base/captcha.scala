package views.html.base

import scala.language.reflectiveCalls

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object captcha {

  private val dataCheckUrl = attr("data-check-url")
  private val dataX        = attr("data-x")
  private val dataY        = attr("data-y")
  private val dataZ        = attr("data-z")

  def apply(form: lila.common.Form.FormLike, captcha: lila.common.Captcha)(implicit ctx: Context) =
    frag(
      form3.hidden(form("gameId"), captcha.gameId.some),
      if (ctx.blind) form3.hidden(form("move"), captcha.solutions.head.some)
      else {
        val url =
          netBaseUrl + routes.Round.watcher(captcha.gameId, if (captcha.sente) "sente" else "gote")
        div(
          cls := List(
            "captcha form-group" -> true,
            "is-invalid"         -> lila.common.Captcha.isFailed(form),
          ),
          dataCheckUrl := routes.Main.captchaCheck(captcha.gameId),
        )(
          div(cls := "challenge")(
            div(
              cls   := s"mini-board ${variantClass(shogi.variant.Minishogi)}",
              dataX := encodeSfen(captcha.hint),
              dataY := encodeSfen(if (captcha.sente) {
                "sente"
              } else {
                "gote"
              }),
              dataZ := encodeSfen(captcha.sfenBoard),
            )(div(cls := "sg-wrap")),
          ),
          div(cls := "captcha-explanation")(
            label(cls := "form-label")(
              transWithColorName(
                trans.xCheckmatesInOneMove,
                shogi.Color.fromSente(captcha.sente),
                false,
              ),
            ),
            br,
            br,
            trans.thisIsAShogiCaptcha(),
            br,
            trans.clickOnTheBoardToMakeYourMove(),
            br,
            br,
            trans.help(),
            " ",
            a(title := trans.viewTheSolution.txt(), target := "_blank", href := s"${url}#last")(
              url,
            ),
            div(cls := "result success text", dataIcon := "E")(trans.checkmate()),
            div(cls := "result failure text", dataIcon := "k")(trans.notACheckmate()),
            form3.hidden(form("move")),
          ),
        )
      },
    )
}
