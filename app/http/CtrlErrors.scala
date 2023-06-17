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

  def apiFormError(form: Form[?]): JsObject =
    Json.obj("error" -> errorsAsJson(form)(using lila.i18n.defaultLang))

  def ridiculousBackwardCompatibleJsonError(err: JsObject): JsObject =
    err ++ Json.obj("error" -> err)

  def jsonFormError(err: Form[?])(using Lang) = fuccess:
    BadRequest(ridiculousBackwardCompatibleJsonError(errorsAsJson(err)))

  def newJsonFormError(err: Form[?])(using Lang) = fuccess:
    BadRequest(errorsAsJson(err))
