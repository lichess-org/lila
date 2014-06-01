package lila.app
package templating

import lila.api.Context
import play.api.data._
import play.twirl.api.Html

trait FormHelper { self: I18nHelper =>

  private val errNames = Map(
    "error.minLength" -> trans.textIsTooShort,
    "error.maxLength" -> trans.textIsTooLong,
    "error.required" -> trans.required
  )

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map { e =>
      """<p class="error">%s</p>""".format(
        (errNames get e.message map (_.str())) | e.message)
    } mkString
  }
}
