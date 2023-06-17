package lila.app
package http

import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.api.context.*
import lila.user.UserContext

trait CtrlConversions:

  export _root_.router.ReverseRouterConversions.given

  given (using ctx: AnyContext): Lang                              = ctx.lang
  given (using ctx: AnyContext): RequestHeader                     = ctx.req
  given (using ctx: AnyContext): UserContext                       = ctx.userContext
  given reqBody(using it: BodyContext[?]): play.api.mvc.Request[?] = it.body

  given (using req: RequestHeader): ui.EmbedConfig = ui.EmbedConfig(req)
