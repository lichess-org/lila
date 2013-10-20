package lila.pref

import lila.pref.Env.{ current ⇒ env }
import lila.user.Context

trait PrefHelper {

  private def get(name: String)(implicit ctx: Context): Option[String] =
    ctx.req.session get name orElse {
      ctx.me ?? { env.api.getPrefString(_, name) }
    }.await

  def currentTheme(implicit ctx: Context): Theme = Theme(~get("theme"))

  def currentBg(implicit ctx: Context): String =
    if (get("dark") == Some("true")) "dark" else "light"

  def userPref(implicit ctx: Context): Pref = {
    ctx.me.fold(fuccess(Pref.default))(env.api.getPref)
  }.await

  def themeList = Theme.list
}
