package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.security.RecaptchaSetup

object recaptcha {

  private val callbackFunction = "window.recaptchaSubmit"

  def script(re: RecaptchaSetup)(implicit ctx: Context) =
    re.enabled option frag(
      raw(
        """<script src="https://www.google.com/recaptcha/api.js" async defer></script>"""
      ),
      embedJsUnsafe(
        s"""$callbackFunction = function(t) { document.getElementById('${re.formId}').submit(); }"""
      )
    )

  def button(re: RecaptchaSetup)(tag: Tag) =
    tag(
      attr("data-sitekey") := re.config.key,
      attr("data-callback") := callbackFunction
    )
}
