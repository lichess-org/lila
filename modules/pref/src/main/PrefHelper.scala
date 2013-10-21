package lila.pref

import lila.pref.Env.{ current ⇒ env }
import lila.user.Context

trait PrefHelper {

  def currentTheme(implicit ctx: Context) = 
    (ctxPref("theme") map Theme.apply) | Pref.default.realTheme

  def currentBg(implicit ctx: Context) = ctxPref("bg") | "light"

  private def ctxPref(name: String)(implicit ctx: Context): Option[String] =
    ctx.req.session get name orElse { userPref get name } 

  def userPref(implicit ctx: Context): Pref = {
    ctx.me.fold(fuccess(Pref.default))(env.api.getPref)
  }.await

  def themeList = Theme.list
}
