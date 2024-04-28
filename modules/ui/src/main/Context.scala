package lila.ui

import play.api.mvc.{ Request, RequestHeader }
import play.api.i18n.Lang

import lila.core.i18n.{ Language, Translate, defaultLanguage }
import lila.core.net.IpAddress
import lila.core.pref.Pref
import lila.core.user.KidMode
import lila.core.notify.UnreadCount

trait Context:
  val req: RequestHeader
  val lang: Lang
  def isAuth: Boolean
  def isOAuth: Boolean
  def me: Option[Me]
  def user: Option[User]
  def userId: Option[UserId]
  def translate: Translate
  def pref: Pref
  def ip: IpAddress
  def blind: Boolean
  def troll: Boolean
  def isBot: Boolean
  def kid: KidMode

  def is[U: UserIdOf](u: U): Boolean      = me.exists(_.is(u))
  def isnt[U: UserIdOf](u: U): Boolean    = !is(u)
  def noBlind                             = !blind
  def flash(name: String): Option[String] = req.flash.get(name)
  inline def noBot                        = !isBot
  lazy val acceptLanguages: Set[Language] =
    req.acceptLanguages.view.map(Language.apply).toSet + defaultLanguage ++
      user.flatMap(_.realLang.map(Language.apply)).toSet

object Context:
  given ctxMe(using ctx: Context): Option[Me] = ctx.me

trait PageContext extends Context:
  val me: Option[Me]
  val needsFp: Boolean
  val impersonatedBy: Option[User]
  // val oauth: Option[TokenScopes]
  def teamNbRequests: Int
  def nbChallenges: Int
  def nbNotifications: UnreadCount
  def hasClas: Boolean
  def hasInquiry: Boolean
  def nonce: Option[Nonce]
  def error: Boolean
