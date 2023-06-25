package lila.app
package http

import play.api.libs.json.{ Reads, JsArray, JsNumber, JsObject, JsString, JsValue, Json, Writes }
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.*

import lila.i18n.{ Translator, I18nKey }

trait CtrlErrors extends ControllerHelpers:

  private val jsonGlobalErrorRenamer: Reads[JsObject] =
    import play.api.libs.json.*
    __.json update (
      (__ \ "global").json copyFrom (__ \ "").json.pick
    ) andThen (__ \ "").json.prune

  def errorsAsJson(form: Form[?])(using lang: Lang): JsObject =
    val json = JsObject:
      form.errors
        .groupBy(_.key)
        .view
        .mapValues: errors =>
          JsArray:
            errors.map: e =>
              JsString(Translator.txt.literal(I18nKey(e.message), e.args, lang))
        .toMap
    json validate jsonGlobalErrorRenamer getOrElse json

  /* This is what we want
   * { "error" -> { "key" -> "value" } }
   */
  def jsonFormErrorBody(form: Form[?]): JsObject =
    Json.obj("error" -> errorsAsJson(form)(using lila.i18n.defaultLang))

  def jsonFormError(form: Form[?])(using Lang) = fuccess:
    BadRequest(jsonFormErrorBody(form))

  /* For compat with old clients
   * { "error" -> { "key" -> "value" }, "key" -> "value" }
   */
  def doubleJsonFormErrorBody(form: JsObject): JsObject =
    form ++ Json.obj("error" -> form)

  def doubleJsonFormError(form: Form[?])(using Lang) = fuccess:
    BadRequest(doubleJsonFormErrorBody(errorsAsJson(form)))

  def badJsonFormError(form: Form[?])(using Lang) = fuccess:
    BadRequest(errorsAsJson(form))
