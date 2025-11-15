package lila.web

import play.api.data.Form
import play.api.libs.json.{ JsArray, JsObject, JsString, Json, Reads, Writes }
import play.api.mvc.*

import lila.core.i18n.{ I18nKey, Translate }

trait CtrlErrors extends ControllerHelpers:

  given Writes[lila.memo.RateLimit.Limited] = Writes: l =>
    Json.obj(
      "error" -> l.msg,
      "ratelimit" -> Json.obj(
        "key" -> l.key,
        "seconds" -> (l.until.toSeconds - nowSeconds).toInt.atLeast(0)
      )
    )

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
              JsString(I18nKey(e.message).txt(e.args*))
        .toMap
    json.validate(using jsonGlobalErrorRenamer).getOrElse(json)

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
