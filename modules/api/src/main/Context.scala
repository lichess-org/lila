package lila.api

import play.api.libs.json.JsObject
import play.api.mvc.{ Request, RequestHeader }

import lila.chat.Chat
import lila.pref.Pref
import lila.user.{ UserContext, HeaderUserContext, BodyUserContext }

sealed trait Context extends lila.user.UserContextWrapper {

  val userContext: UserContext
  val chat: Option[Chat]
  val friends: Option[JsObject]

  val pref: Pref

  def currentTheme =
    (ctxPref("theme") map lila.pref.Theme.apply) | Pref.default.realTheme

  def currentBg = ctxPref("bg") | "light"

  private def ctxPref(name: String): Option[String] =
    userContext.req.session get name orElse { pref get name }
}

sealed abstract class BaseContext(
  val userContext: lila.user.UserContext,
  val chat: Option[Chat],
  val pref: Pref,
  val friends: Option[JsObject]) extends Context

final class BodyContext(
  val bodyContext: BodyUserContext,
  chatOption: Option[Chat],
  p: Pref,
  f: Option[JsObject])
    extends BaseContext(bodyContext, chatOption, p, f) {

  def body = bodyContext.body
}

final class HeaderContext(
  val headerContext: HeaderUserContext,
  chatOption: Option[Chat],
  p: Pref,
  f: Option[JsObject])
    extends BaseContext(headerContext, chatOption, p, f)

object Context {

  def apply(req: RequestHeader): HeaderContext =
    new HeaderContext(UserContext(req, none), none, Pref.default, none)

  def apply(userContext: HeaderUserContext, chat: Option[Chat], pref: Option[Pref], friends: Option[JsObject]): HeaderContext =
    new HeaderContext(userContext, chat, pref | Pref.default, friends)

  def apply(userContext: BodyUserContext, chat: Option[Chat], pref: Option[Pref], friends: Option[JsObject]): BodyContext =
    new BodyContext(userContext, chat, pref | Pref.default, friends)
}
