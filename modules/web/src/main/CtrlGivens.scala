package lila.web

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import scalalib.net.UserAgent

import lila.common.{ ClientName, HTTPRequest }
import lila.core.i18n.Translate
import lila.core.pref.Pref
import lila.ui.Context

trait CtrlGivens:

  given (using ctx: Context): Lang = ctx.lang
  given (using ctx: Context): Translate = ctx.translate
  given (using ctx: Context): RequestHeader = ctx.req
  given (using ctx: Context): Pref = ctx.pref
  given (using req: RequestHeader): UserAgent = HTTPRequest.userAgent(req)
  given (using req: RequestHeader): ClientName = ClientName(req)
