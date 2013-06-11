package lila.app
package templating

import lila.user.{ Theme, Setting, Context }

import play.api.templates.Html

trait SettingHelper {

  def setting(implicit ctx: Context) = new Setting(ctx)

  def themeList = Theme.list
}
