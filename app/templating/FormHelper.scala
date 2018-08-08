package lidraughts.app
package templating

import play.api.data._
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.i18n.I18nDb

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Html = Html {
    s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args)}</p>"""
  }

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map errMsg mkString
  }

  def errMsgMaterial(errors: Seq[FormError])(implicit ctx: Context): Option[Html] = errors.nonEmpty option Html {
    val msgs = errors.map { error =>
      s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args)}</p>"""
    } mkString ""
    s"""<div class="form-group has-error">$msgs</div>"""
  }

  def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map errMsg

  def globalErrorMaterial(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map { msg =>
      Html(s"""<div class="form-group has-error">${errMsg(msg)}</div>""")
    }

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")
}
