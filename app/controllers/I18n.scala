package controllers

import lila.app._
import views._
import lila.user.Context
import lila.i18n.{ Translation, TransInfo }
import lila.common.{ Captcha, LilaCookie }

import play.api.data.Form

object I18n extends LilaController {

  private def env = Env.i18n

  def contribute = Open { implicit ctx ⇒
    val mines = (ctx.req.acceptLanguages map env.transInfos.get).toList.flatten
    Ok(html.i18n.contribute(env.transInfos.all, mines)).fuccess
  }

  def translationForm(lang: String) = Open { implicit ctx ⇒
    OptionFuOk(fuccess(env.transInfos get lang)) { info ⇒
      env.forms.translationWithCaptcha map {
        case (form, captcha) ⇒ renderTranslationForm(form, info, captcha)
      }
    }
  }

  def translationPost(lang: String) = OpenBody { implicit ctx ⇒
    OptionFuResult(fuccess(env.transInfos get lang)) { info ⇒
      implicit val req = ctx.body
      val data = env.forms.decodeTranslationBody
      FormFuResult(env.forms.translation) { form ⇒
        env.forms.anyCaptcha map { captcha ⇒
          renderTranslationForm(form, info, captcha, data)
        }
      } { metadata ⇒
        env.forms.process(lang, metadata, data) inject
          Redirect(routes.I18n.contribute).flashing("success" -> "1")
      }
    }
  }

  private def renderTranslationForm(form: Form[_], info: TransInfo, captcha: Captcha, data: Map[String, String] = Map.empty)(implicit ctx: Context) =
    html.i18n.translationForm(
      info,
      form,
      env.keys,
      env.pool.default,
      env.translator.rawTranslation(info.lang) _,
      captcha,
      data)

  def fetch(from: Int) = Open { implicit ctx ⇒
    JsonOk(env jsonFromVersion from)
  }

  def hideCalls = Open { implicit ctx ⇒
    implicit val req = ctx.req
    val cookie = LilaCookie.cookie(
      env.hideCallsCookieName,
      "1",
      maxAge = env.hideCallsCookieMaxAge.some)
    fuccess(Redirect(routes.Lobby.home()) withCookies cookie)
  }
}
