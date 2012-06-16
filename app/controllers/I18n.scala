package controllers

import lila._
import views._

import i18n._

object I18n extends LilaController {

  def transInfos = env.i18n.transInfos
  def pool = env.i18n.pool
  def forms = i18n.DataForm
  def i18nKeys = env.i18n.keys

  val contribute = Open { implicit ctx ⇒
    val mines = (pool fixedReqAcceptLanguages ctx.req map { lang ⇒
      transInfos get lang
    }).toList.flatten
    Ok(html.i18n.contribute(transInfos.all, mines))
  }

  def translationForm(lang: String) = Open { implicit ctx ⇒
    OptionOk(transInfos get lang) { info ⇒
      html.i18n.translationForm(info, forms.translation, i18nKeys)
    }
  }
}
