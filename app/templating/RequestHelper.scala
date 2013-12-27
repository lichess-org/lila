package lila.app
package templating

import lila.api.Context

trait RequestHelper {

  def currentUrl(implicit ctx: Context) = ctx.req.host + ctx.req.uri
}
