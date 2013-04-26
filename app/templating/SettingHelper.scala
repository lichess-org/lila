package lila.app
package templating

import lila.user.{ Theme, Setting, Context }

import play.api.templates.Html

trait SettingHelper {

  def setting(implicit ctx: Context) = new Setting(ctx)

  def soundString(implicit ctx: Context) = 
    setting(ctx).sound.fold("sound_state_on", "")

  def themeList = Theme.list
}
