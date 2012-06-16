package controllers

import lila._
import views._
import http.LilaCookie

import i18n._

object I18n extends LilaController {

  def transInfos = env.i18n.transInfos
  def pool = env.i18n.pool
  def translator = env.i18n.translator
  def forms = env.i18n.forms
  def i18nKeys = env.i18n.keys
  def repo = env.i18n.translationRepo

  val contribute = Open { implicit ctx ⇒
    val mines = (pool fixedReqAcceptLanguages ctx.req map { lang ⇒
      transInfos get lang
    }).toList.flatten
    Ok(html.i18n.contribute(transInfos.all, mines))
  }

  def translationForm(lang: String) = Open { implicit ctx ⇒
    OptionOk(transInfos get lang) { info ⇒
      html.i18n.translationForm(
        info,
        forms.translation,
        i18nKeys,
        pool.default,
        translator.rawTranslation(info.lang) _)
    }
  }

  def translationPost(lang: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    FormIOResult(forms.translation) {
      case metadata ⇒ forms.process(lang, metadata) map { _ ⇒
        Redirect(routes.I18n.contribute).flashing("success" -> "1")
      }
    }
  }

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
