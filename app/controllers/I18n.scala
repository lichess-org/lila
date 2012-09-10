package controllers

import lila._
import views._
import site.Captcha
import http.Context
import http.LilaCookie

import play.api.data.Form
import i18n._

object I18n extends LilaController {

  private def transInfos = env.i18n.transInfos
  private def pool = env.i18n.pool
  private def translator = env.i18n.translator
  private def forms = env.i18n.forms
  private def i18nKeys = env.i18n.keys
  private def repo = env.i18n.translationRepo

  val contribute = Open { implicit ctx ⇒
    val mines = (pool fixedReqAcceptLanguages ctx.req map { lang ⇒
      transInfos get lang
    }).toList.flatten
    Ok(html.i18n.contribute(transInfos.all, mines))
  }

  def translationForm(lang: String) = Open { implicit ctx ⇒
    OptionOk(transInfos get lang) { info ⇒
      val (form, captcha) = forms.translationWithCaptcha
      renderTranslationForm(form, info, captcha)
    }
  }

  def translationPost(lang: String) = OpenBody { implicit ctx ⇒
    OptionResult(transInfos get lang) { info ⇒
      implicit val req = ctx.body
      val data = forms.decodeTranslationBody
      FormIOResult(forms.translation) { form ⇒
        renderTranslationForm(form, info, forms.captchaCreate, data)
      } { metadata ⇒
        forms.process(lang, metadata, data) map { _ ⇒
          Redirect(routes.I18n.contribute).flashing("success" -> "1")
        }
      }
    }
  }

  private def renderTranslationForm(form: Form[_], info: TransInfo, captcha: Captcha.Challenge, data: Map[String, String] = Map.empty)(implicit ctx: Context) =
    html.i18n.translationForm(
      info,
      form,
      i18nKeys,
      pool.default,
      translator.rawTranslation(info.lang) _,
      captcha,
      data)

  def fetch(from: Int) = Open { implicit ctx ⇒
    JsonOk((repo findFrom from map {
      _ map (_.toMap)
    }).unsafePerformIO)
  }

  val hideCalls = Open { implicit ctx ⇒
    implicit val req = ctx.req
    val cookie = LilaCookie.cookie(
      env.i18n.hideCallsCookieName,
      "1",
      maxAge = env.i18n.hideCallsCookieMaxAge.some)
    Redirect(routes.Lobby.home()) withCookies cookie
  }
}
