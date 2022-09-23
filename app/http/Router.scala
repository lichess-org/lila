package lila.app
package http

import play.api.mvc.RequestHeader
import play.core.routing.RouteParams
import play.core.routing.PathPattern

object Route {

  /** Extractor of route from a request.
    */
  trait ParamsExtractor {
    def unapply(request: RequestHeader): Option[RouteParams]
  }

  /** Create a params extractor from the given method and path pattern.
    */
  def apply(method: String, pathPattern: PathPattern) = new ParamsExtractor {
    def unapply(request: RequestHeader): Option[RouteParams] = {
      if (method == request.method) {
        println(s"${request.path} $pathPattern")
        pathPattern(request.path).map { groups =>
          RouteParams(groups, request.queryString)
        }
      } else {
        None
      }
    }
  }
}
