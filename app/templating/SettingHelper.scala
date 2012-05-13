package lila
package templating

import http.{ Context, Setting }

import play.api.templates.Html

trait SettingHelper {

  def setting(implicit ctx: Context) = new Setting(ctx)
}
