package lila
package templating

import play.api.mvc.RequestHeader

trait RequestHelper {

  def currentUrl(implicit req: RequestHeader) = req.domain + req.uri
}
