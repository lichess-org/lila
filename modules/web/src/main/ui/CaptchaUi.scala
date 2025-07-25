package lila.web
package ui

import play.api.data.{ Field, Form }
import play.api.libs.json.Json

import lila.core.captcha.Captcha
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class CaptchaUi(helpers: Helpers):
  import helpers.{ *, given }

  private val dataCheckUrl = attr("data-check-url")
  private val dataMoves = attr("data-moves")
  private val dataPlayable = attr("data-playable")

  def apply(form: Form[?] | Field, captcha: Captcha)(using ctx: Context) =
    frag(
      form3.hidden(formField(form, "gameId"), captcha.gameId.value.some),
      if ctx.blind then form3.hidden(formField(form, "move"), captcha.solutions.head.some)
      else
        val url = s"$netBaseUrl${routes.Round.watcher(captcha.gameId, captcha.color)}"
        div(
          cls := List(
            "captcha form-group" -> true,
            "is-invalid" -> formErrors(form).exists(_.messages.has(lila.core.captcha.failMessage))
          ),
          dataCheckUrl := routes.Main.captchaCheck(captcha.gameId)
        )(
          div(cls := "challenge")(
            chessgroundMini(captcha.fen, captcha.color) {
              div(
                dataMoves := safeJsonValue(Json.toJson(captcha.moves)),
                dataPlayable := 1
              )
            }
          ),
          div(cls := "captcha-explanation")(
            label(cls := "form-label")(
              if captcha.color.white then trans.site.whiteCheckmatesInOneMove()
              else trans.site.blackCheckmatesInOneMove()
            ),
            br,
            br,
            trans.site.thisIsAChessCaptcha(),
            br,
            trans.site.clickOnTheBoardToMakeYourMove(),
            br,
            br,
            trans.site.help(),
            " ",
            a(title := trans.site.viewTheSolution.txt(), targetBlank, href := s"${url}#last")(url),
            div(cls := "result success text", dataIcon := Icon.Checkmark)(trans.site.checkmate()),
            div(cls := "result failure text", dataIcon := Icon.NotAllowed)(trans.site.notACheckmate()),
            form3.hidden(formField(form, "move"))
          )
        )
    )

  def hiddenEmpty(form: Form[?] | Field) = frag(
    form3.hidden(formField(form, "gameId")),
    form3.hidden(formField(form, "move"))
  )

  private def formField(form: Form[?] | Field, name: String) = form match
    case f: Form[?] => f(name)
    case f: Field => f(name)
  private def formErrors(form: Form[?] | Field) = form match
    case f: Form[?] => f.errors
    case f: Field => f.errors
