package lila.app
package http

import play.api.data.Form
import play.api.i18n.Lang
import play.api.libs.json.{ JsArray, JsObject, JsString, Json, Reads, Writes }
import play.api.mvc.*

import lila.core.i18n.I18nKey
import lila.core.i18n.Translate

trait CtrlErrors extends ControllerHelpers:

  def jsonError[A: Writes](err: A): JsObject = Json.obj("error" -> err)

  def notFoundJson(msg: String = "Not found"): Result = NotFound(jsonError(msg)).as(JSON)
  def notFoundText(msg: String = "Not found"): Result = Results.NotFound(msg)

  def forbiddenJson(msg: String = "You can't do that"): Result = Forbidden(jsonError(msg)).as(JSON)
  def forbiddenText(msg: String = "You can't do that"): Result = Results.Forbidden(msg)

  private val jsonGlobalErrorRenamer: Reads[JsObject] =
    import play.api.libs.json.*
    __.json
      .update(
        (__ \ "global").json.copyFrom((__ \ "").json.pick)
      )
      .andThen((__ \ "").json.prune)

  def errorsAsJson(form: Form[?])(using t: Translate): JsObject =
    val json = JsObject:
      form.errors
        .groupBy(_.key)
        .view
        .mapValues: errors =>
          JsArray:
            errors.map: e =>
              JsString(lila.i18n.Translator.txt.literal(I18nKey(e.message), e.args, t.lang))
        .toMap
    json.validate(jsonGlobalErrorRenamer).getOrElse(json)

  /* This is what we want
   * { "error" -> { "key" -> "value" } }
   */
  def jsonFormError(form: Form[?])(using Translate): Result =
    BadRequest(jsonError(errorsAsJson(form)))

  /* For compat with old clients
   * { "error" -> { "key" -> "value" }, "key" -> "value" }
   */
  def doubleJsonFormErrorBody(form: Form[?])(using Translate): JsObject =
    val json = errorsAsJson(form)
    json ++ jsonError(json)

  def doubleJsonFormError(form: Form[?])(using Translate) =
    BadRequest(doubleJsonFormErrorBody(form))

  def badJsonFormError(form: Form[?])(using Translate) =
    BadRequest(errorsAsJson(form))
