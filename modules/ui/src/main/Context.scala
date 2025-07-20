package lila.ui

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import scalalib.model.Language

import lila.core.i18n.{ toLanguage, Translate, defaultLanguage }
import lila.core.net.IpAddress
import lila.core.notify.UnreadCount
import lila.core.pref.Pref
import lila.core.user.KidMode

/* Data available in every HTTP request */
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
  def myId: Option[MyId]                  = me.map(_.myId)
  def noBlind                             = !blind
  def flash(name: String): Option[String] = req.flash.get(name)
  inline def noBot                        = !isBot
  lazy val acceptLanguages: Set[Language] =
    req.acceptLanguages.view.map(toLanguage).toSet + defaultLanguage ++
      user.flatMap(_.realLang.map(toLanguage)).toSet

object Context:
  given ctxMe(using ctx: Context): Option[Me] = ctx.me

/* data necessary to render the lichess website layout */
trait PageContext extends Context:
  val me: Option[Me]
  val needsFp: Boolean
  val impersonatedBy: Option[lila.core.userId.ModId]
  def teamNbRequests: Int
  def nbChallenges: Int
  def nbNotifications: UnreadCount
  def hasClas: Boolean
  def hasInquiry: Boolean
  def nonce: Option[Nonce]
  def error: Boolean
