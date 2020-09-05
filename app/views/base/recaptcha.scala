package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.security.RecaptchaForm

object recaptcha {

  private val callbackFunction = "recaptchaSubmit"

  def script(re: RecaptchaForm[_])(implicit ctx: Context) =
    re.enabled option frag(
      raw(
        """<script src="https://www.google.com/recaptcha/api.js" async defer></script>"""
      ),
      embedJsUnsafe(
        s"""$callbackFunction=t=>document.getElementById('${re.formId}').submit()"""
      )
    )

  def button(re: RecaptchaForm[_])(tag: Tag) =
    tag(
      cls := "g-recaptcha",
      attr("data-sitekey") := re.config.key,
      attr("data-callback") := callbackFunction
    )
}
