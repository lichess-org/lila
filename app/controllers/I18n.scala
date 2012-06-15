package controllers

import lila._
import views._

import i18n._

object I18n extends LilaController {

  def transInfos = env.i18n.transInfos
  def pool = env.i18n.pool

  val contribute = Open { implicit ctx ⇒
    val all = transInfos
    val mines = (pool fixedReqAcceptLanguages ctx.req map { lang ⇒
      all find (_.lang == lang)
    }).toList.flatten
    Ok(html.i18n.contribute(all, mines))
  }

  def contributeTrans(lang: String) = TODO //Open { implicit ctx =>
}
