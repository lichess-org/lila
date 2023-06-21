package lila.api

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }

import lila.common.HTTPRequest
import lila.pref.Pref
import lila.user.{ Me, MyId, User }
import lila.notify.Notification.UnreadCount
import lila.oauth.{ OAuthScope, TokenScopes }

object Context:
  export lila.api.{ Context, BodyContext }
  export lila.api.{ LoginContext, PageContext }
  given (using ctx: Context): Option[Me]   = ctx.me
  given (using ctx: Context): Option[MyId] = ctx.meId
  given (using page: PageContext): Context = page.ctx

/* Who is logged in, and how */
final class LoginContext(
    val me: Option[Me],
    val needsFp: Boolean,
    val impersonatedBy: Option[User],
    val oauth: Option[TokenScopes]
):
  export me.{ isDefined as isAuth, isEmpty as isAnon }
  def meId: Option[MyId]             = me.map(_.meId)
  def is[U: UserIdOf](u: U): Boolean = me.exists(_ is u)
  inline def user: Option[User]      = Me raw me
  def userId: Option[UserId]         = user.map(_.id)
  def username: Option[UserName]     = user.map(_.username)
  def isBot                          = me.exists(_.isBot)
  def noBot                          = !isBot
  def troll                          = user.exists(_.marks.troll)
  def kid                            = user.exists(_.kid)
  def noKid                          = !kid
  def isAppealUser                   = me.exists(_.enabled.no)
  def isWebAuth                      = isAuth && oauth.isEmpty
  def isOAuth                        = isAuth && oauth.isDefined
  def isMobile                       = oauth.exists(_.has(_.Web.Mobile))
  def scopes                         = oauth | TokenScopes(Nil)

object LoginContext:
  val anon = LoginContext(none, false, none, none)

/* Data available in every HTTP request */
class Context(
    val req: RequestHeader,
    val lang: Lang,
    val userContext: LoginContext,
    val pref: Pref
):
  export userContext.*
  def ip                    = HTTPRequest ipAddress req
  lazy val blind            = req.cookies.get(ApiConfig.blindCookie.name).exists(_.value.nonEmpty)
  def noBlind               = !blind
  lazy val mobileApiVersion = Mobile.Api requestVersion req
  def isMobileApi           = mobileApiVersion.isDefined
  def flash(name: String): Option[String] = req.flash get name
  def withLang(l: Lang)                   = new Context(req, l, userContext, pref)

final class BodyContext[A](
    val body: Request[A],
    lang: Lang,
    userContext: LoginContext,
    pref: Pref
) extends Context(body, lang, userContext, pref)

/* data necessary to render the lichess website layout */
case class PageData(
    teamNbRequests: Int,
    nbChallenges: Int,
    nbNotifications: UnreadCount,
    hasClas: Boolean,
    inquiry: Option[lila.mod.Inquiry],
    nonce: Option[Nonce],
    error: Boolean = false
)

object PageData:
  def anon(nonce: Option[Nonce])  = PageData(0, 0, UnreadCount(0), false, none, nonce)
  def error(nonce: Option[Nonce]) = anon(nonce).copy(error = true)

final class PageContext(val ctx: Context, val data: PageData):
  export ctx.*
  export data.*
