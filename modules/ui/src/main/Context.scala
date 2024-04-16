package lila.ui

import play.api.mvc.{ Request, RequestHeader }
import play.api.i18n.Lang

import lila.core.i18n.{ Language, Translate, defaultLanguage }
import lila.core.net.IpAddress

trait Context:
  val req: RequestHeader
  val lang: Lang
  def isAuth: Boolean
  def isOAuth: Boolean
  def me: Option[Me]
  def user: Option[User]
  def translate: Translate

  def ip: IpAddress
  def blind: Boolean
  def noBlind                             = !blind
  def flash(name: String): Option[String] = req.flash.get(name)
  lazy val acceptLanguages: Set[Language] =
    req.acceptLanguages.view.map(Language.apply).toSet + defaultLanguage ++
      user.flatMap(_.realLang.map(Language.apply)).toSet
