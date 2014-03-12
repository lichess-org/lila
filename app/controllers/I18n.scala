package controllers

import play.api.data.Form

import lila.app._
import lila.common.{ Captcha, LilaCookie }
import lila.i18n.{ Translation, TransInfo }
import lila.api.Context
import views._

object I18n extends LilaController {

  private def env = Env.i18n

  def contribute = Open { implicit ctx =>
    val mines = (ctx.req.acceptLanguages map env.transInfos.get).toList.flatten.distinct
    Ok(html.i18n.contribute(env.transInfos.all, mines)).fuccess
  }

  def translationForm(lang: String) = Auth { implicit ctx =>
    me =>
      OptionFuOk(infoAndContext(lang)) {
        case (info, context) => env.forms.translationWithCaptcha map {
          case (form, captcha) => renderTranslationForm(form, info, captcha, context = context)
        }
      }
  }

  def translationPost(lang: String) = AuthBody { implicit ctx =>
    me =>
      OptionFuResult(infoAndContext(lang)) {
        case (info, context) =>
          implicit val req = ctx.body
          val data = env.forms.decodeTranslationBody
          FormFuResult(env.forms.translation) { form =>
            env.forms.anyCaptcha map { captcha =>
              renderTranslationForm(form, info, captcha, data = data, context = context)
            }
          } { metadata =>
            env.forms.process(lang, metadata, data, me.username) inject
              Redirect(routes.I18n.contribute).flashing("success" -> "1")
          }
      }
  }

  private def infoAndContext(lang: String) = env.transInfos.get(lang) ?? { i =>
    env.context.get map (i -> _) map (_.some)
  }

  private def renderTranslationForm(
    form: Form[_],
    info: TransInfo,
    captcha: Captcha,
    context: Map[String, String],
    data: Map[String, String] = Map.empty)(implicit ctx: Context) =
    html.i18n.translationForm(
      info,
      form,
      env.keys,
      env.pool.default,
      env.translator.rawTranslation(info.lang) _,
      captcha,
      data = data,
      context = context)

  def fetch(from: Int) = Open { implicit ctx =>
    JsonOk(env jsonFromVersion from)
  }

  def hideCalls = Open { implicit ctx =>
    implicit val req = ctx.req
    val cookie = LilaCookie.cookie(
      env.hideCallsCookieName,
      "1",
      maxAge = env.hideCallsCookieMaxAge.some)
    fuccess(Redirect(routes.Lobby.home()) withCookies cookie)
  }
}
