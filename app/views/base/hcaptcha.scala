package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.security.HcaptchaForm

object hcaptcha {

  private val dataSitekey = attr("data-sitekey")

  def script(re: HcaptchaForm[_])(implicit ctx: Context) =
    re.enabled option raw("""<script src="https://hcaptcha.com/1/api.js" async defer></script>""")

  def tag(form: HcaptchaForm[_]) =
    div(cls := "h-captcha form-group", dataSitekey := form.config.key)
}
