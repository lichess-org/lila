package lila.web

import play.api.mvc.{ Request, RequestHeader }
import play.api.i18n.Lang

import lila.common.HTTPRequest
import lila.core.i18n.{ Language, defaultLanguage }

trait Context:
  val req: RequestHeader
  val lang: Lang
  def isAuth: Boolean
  def me: Option[Me]
  def user: Option[User]

  def ip         = HTTPRequest.ipAddress(req)
  lazy val blind = req.cookies.get(lila.web.WebConfig.blindCookie.name).exists(_.value.nonEmpty)
  def noBlind    = !blind
  def flash(name: String): Option[String] = req.flash.get(name)
  lazy val acceptLanguages: Set[Language] =
    req.acceptLanguages.view.map(Language.apply).toSet + defaultLanguage ++
      user.flatMap(_.realLang.map(Language.apply)).toSet
