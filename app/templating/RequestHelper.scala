package lila.app
package templating

import lila.api.Context

trait RequestHelper {

  def currentPath(implicit ctx: Context) = ctx.req.path
}
