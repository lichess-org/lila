package lila
package templating

import play.api.data._
import play.api.templates.Html

trait FormHelper {

  private val errNames = Map(
    "error.minLength" -> "Text is too short.",
    "error.required" -> "Required."
  )

  def errMsg(form: Field): Html = errMsg(form.errors)

  def errMsg(form: Form[_]): Html = errMsg(form.errors)

  def errMsg(errors: Seq[FormError]): Html = Html {
    errors map { e ⇒ 
    """<p class="error">%s</p>""".format(
      (errNames get e.message) | e.message)
    } mkString
  }
}
