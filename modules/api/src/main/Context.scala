package lila.api

import play.api.mvc.{ Request, RequestHeader }

import lila.chat.NamedChat
import lila.pref.Pref
import lila.user.{ UserContext, HeaderUserContext, BodyUserContext }

sealed trait Context extends lila.user.UserContextWrapper {

  val userContext: UserContext
  val chat: Option[NamedChat]
  val pref: Pref

  def currentTheme =
    (ctxPref("theme") map lila.pref.Theme.apply) | Pref.default.realTheme

  def currentBg = ctxPref("bg") | "light"

  private def ctxPref(name: String): Option[String] =
    userContext.req.session get name orElse { pref get name }
}

sealed abstract class BaseContext(
  val userContext: lila.user.UserContext,
  val chat: Option[NamedChat],
  val pref: Pref) extends Context

final class BodyContext(val bodyContext: BodyUserContext, chatOption: Option[NamedChat], p: Pref)
    extends BaseContext(bodyContext, chatOption, p) {
  def body = bodyContext.body
}

final class HeaderContext(val headerContext: HeaderUserContext, chatOption: Option[NamedChat], p: Pref)
  extends BaseContext(headerContext, chatOption, p)

object Context {

  def apply(req: RequestHeader): HeaderContext =
    new HeaderContext(UserContext(req, none), none, Pref.default)

  def apply(userContext: HeaderUserContext, chat: Option[NamedChat], pref: Option[Pref]): HeaderContext =
    new HeaderContext(userContext, chat, pref | Pref.default)

  def apply(userContext: BodyUserContext, chat: Option[NamedChat], pref: Option[Pref]): BodyContext =
    new BodyContext(userContext, chat, pref | Pref.default)
}
