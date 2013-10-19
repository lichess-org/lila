package lila.app
package templating

import lila.user.Context
import lila.pref.{ Theme, SessionAware }
import lila.pref.Env.{ current ⇒ prevEnv }

import play.api.templates.Html

trait PrefHelper {

  def pref(implicit ctx: Context) = new SessionAware(prevEnv.api)

  def themeList = Theme.list
}
