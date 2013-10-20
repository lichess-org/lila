package lila.pref

import lila.pref.Env.{ current ⇒ env }
import lila.user.Context

trait PrefHelper {

  def currentTheme(implicit ctx: Context) = userPref.realTheme

  def currentBg(implicit ctx: Context) = userPref.dark.fold("dark", "light")

  def userPref(implicit ctx: Context): Pref = {
    ctx.me.fold(fuccess(Pref.default))(env.api.getPref)
  }.await

  def themeList = Theme.list
}
