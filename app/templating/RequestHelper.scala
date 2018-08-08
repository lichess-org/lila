package lidraughts.app
package templating

import lidraughts.api.Context

trait RequestHelper {

  def currentPath(implicit ctx: Context) = ctx.req.path
}
