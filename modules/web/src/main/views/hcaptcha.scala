package lila.web
package views

import lila.ui.ScalatagsTemplate.*
import lila.core.security.HcaptchaForm

object hcaptcha:

  def script(re: HcaptchaForm[?]) =
    re.enabled.option(raw("""<script src="https://hcaptcha.com/1/api.js" async defer></script>"""))
