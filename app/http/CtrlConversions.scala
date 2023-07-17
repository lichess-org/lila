package lila.app
package http

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }
import lila.user.User

trait CtrlConversions:

  export _root_.router.ReverseRouterConversions.given

  given (using ctx: Context): Lang                    = ctx.lang
  given (using ctx: Context): RequestHeader           = ctx.req
  given reqBody(using it: BodyContext[?]): Request[?] = it.body

  given Conversion[User.WithPerfs, User] = _.user
