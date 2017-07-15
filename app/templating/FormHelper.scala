package lila.app
package templating

import play.api.data._
import play.twirl.api.Html

import lila.api.Context
import lila.i18n.{ I18nKeys, I18nDb }

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Html = Html {
    s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args)}</p>"""
  }

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map errMsg mkString
  }

  def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map errMsg

  def globalErrorMaterial(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map { msg =>
      Html(s"""<div class="form-group has-error">${errMsg(msg)}</div>""")
    }

  val booleanChoices = Seq("true" -> "Yes", "false" -> "No")
}
