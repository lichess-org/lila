package lila.app
package templating

import lila.user.Context
import lila.pref.Theme
import lila.pref.Env.{ current ⇒ prevEnv }

import play.api.templates.Html

trait PrefHelper {

  private def get(name: String)(implicit ctx: Context): Option[String] = 
    ctx.req.session get name orElse {
      ctx.me ?? { prevEnv.api.getPrefString(_, name) }
    }.await 

  def currentTheme(implicit ctx: Context): Theme = Theme(~get("theme"))

  def currentBg(implicit ctx: Context): String = 
    if (get("dark") == Some("true")) "dark" else "light"

  def themeList = Theme.list
}
