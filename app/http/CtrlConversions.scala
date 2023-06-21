package lila.app
package http

import play.api.i18n.Lang
import play.api.mvc.RequestHeader

trait CtrlConversions:

  export _root_.router.ReverseRouterConversions.given

  given (using ctx: Context): Lang                                 = ctx.lang
  given (using ctx: Context): RequestHeader                        = ctx.req
  given reqBody(using it: BodyContext[?]): play.api.mvc.Request[?] = it.body
