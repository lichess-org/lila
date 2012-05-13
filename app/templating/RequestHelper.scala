package lila
package templating

import http.Context

trait RequestHelper {

  def currentUrl(implicit ctx: Context) = ctx.req.domain + ctx.req.uri
}
