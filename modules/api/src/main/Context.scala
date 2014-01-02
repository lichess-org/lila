package lila.api

import play.api.libs.json.{ JsObject, JsArray }
import play.api.mvc.{ Request, RequestHeader }

import lila.chat.Chat
import lila.pref.Pref
import lila.user.{ UserContext, HeaderUserContext, BodyUserContext }

case class PageData(
  chat: Option[Chat],
  friends: Option[JsObject],
  pref: Pref,
  teams: Option[JsArray])

object PageData {
  val default = PageData(none, none, Pref.default, none)
}

sealed trait Context extends lila.user.UserContextWrapper {

  val userContext: UserContext
  val pageData: PageData

  def chat = pageData.chat
  def friends = pageData.friends
  def pref = pageData.pref
  def teams = pageData.teams

  def currentTheme =
    (ctxPref("theme") map lila.pref.Theme.apply) | Pref.default.realTheme

  def currentBg = ctxPref("bg") | "light"

  private def ctxPref(name: String): Option[String] =
    userContext.req.session get name orElse { pref get name }
}

sealed abstract class BaseContext(
  val userContext: lila.user.UserContext,
  val pageData: PageData) extends Context

final class BodyContext(
    val bodyContext: BodyUserContext,
    data: PageData) extends BaseContext(bodyContext, data) {

  def body = bodyContext.body
}

final class HeaderContext(
  headerContext: HeaderUserContext,
  data: PageData) extends BaseContext(headerContext, data)

object Context {

  def apply(req: RequestHeader): HeaderContext =
    new HeaderContext(UserContext(req, none), PageData.default)

  def apply(userContext: HeaderUserContext, pageData: PageData): HeaderContext =
    new HeaderContext(userContext, pageData)

  def apply(userContext: BodyUserContext, pageData: PageData): BodyContext =
    new BodyContext(userContext, pageData)
}
