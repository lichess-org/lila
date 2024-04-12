package lila.app
package http

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }
import lila.core.i18n.Translate
import lila.core.perf.UserWithPerfs
import lila.core.user.User

trait CtrlConversions:

  export _root_.router.ReverseRouterConversions.given

  given (using ctx: Context): Lang                    = ctx.lang
  given (using ctx: Context): Translate               = ctx.translate
  given (using ctx: Context): RequestHeader           = ctx.req
  given reqBody(using it: BodyContext[?]): Request[?] = it.body

  given Conversion[UserWithPerfs, User] = _.user
