package lila.app
package templating

import play.api.data._
import play.twirl.api.Html

import lila.api.Context
import lila.i18n.I18nKeys

trait FormHelper { self: I18nHelper =>

  private val errNames = Map(
    "error.minLength" -> I18nKeys.textIsTooShort,
    "error.maxLength" -> I18nKeys.textIsTooLong,
    "captcha.fail" -> I18nKeys.notACheckmate
  )

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map { e =>
      val msg = transKey(e.message, e.args) match {
        case m if m == e.message => errNames.get(e.message).fold(e.message)(_.txt())
        case m => m
      }
      s"""<p class="error">$msg</p>"""
    } mkString
  }

  def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError.map { e =>
      val msg = transKey(e.message, e.args)
      Html(s"""<p class="error">$msg</p>""")
    }

  val booleanChoices = Seq("true" -> "Yes", "false" -> "No")
}
