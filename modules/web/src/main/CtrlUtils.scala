package lila.web

import play.api.data.*
import play.api.data.Forms.*
import scalalib.StringOps.addQueryParam

import lila.common.HTTPRequest

object CtrlUtils:

  val blindForm = Form:
    tuple(
      "enable" -> nonEmptyText,
      "redirect" -> nonEmptyText
    )

  def authenticationUrl(using req: Req): String =
    val url =
      if ClosedLogin.acceptsPath(req)
      then routes.Auth.login.url
      else
        HTTPRequest.queryStringGet("login") match
          case Some(login) => s"${routes.Auth.login.url}?as=$login"
          case _ =>
            HTTPRequest.actionName(req) match
              case "Recap.home" => routes.Auth.login.url
              case _ => routes.Auth.signup.url
    addQueryParam(url, "referrer", req.path)
