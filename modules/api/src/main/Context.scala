package lila.api

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }

import lila.common.HTTPRequest
import lila.pref.Pref
import lila.user.{ UserBodyContext, UserContext }
import lila.notify.Notification.UnreadCount
import lila.oauth.{ OAuthScope, OAuthScopes }

object context:
  export lila.api.{ AnyContext, BodyContext }
  export lila.api.{ WebContext, WebBodyContext }
  export lila.api.{ OAuthContext, OAuthBodyContext }
  export lila.api.{ MinimalContext, MinimalBodyContext }

/* data necessary to render the lichess website layout */
case class PageData(
    teamNbRequests: Int,
    nbChallenges: Int,
    nbNotifications: UnreadCount,
    pref: Pref,
    blindMode: Boolean,
    hasFingerprint: Boolean,
    hasClas: Boolean,
    inquiry: Option[lila.mod.Inquiry],
    nonce: Option[Nonce],
    error: Boolean = false
)

object PageData:

  def anon(req: RequestHeader, nonce: Option[Nonce], blindMode: Boolean = false) =
    PageData(
      teamNbRequests = 0,
      nbChallenges = 0,
      nbNotifications = UnreadCount(0),
      lila.pref.RequestPref fromRequest req,
      blindMode = blindMode,
      hasFingerprint = false,
      hasClas = false,
      inquiry = none,
      nonce = nonce
    )

  def error(req: RequestHeader, nonce: Option[Nonce]) = anon(req, nonce).copy(error = true)

trait AnyContext:
  val userContext: UserContext
  export userContext.{ req, me, impersonatedBy, lang, userId, is, kid, noKid, troll }
  export me.{ isDefined as isAuth, isEmpty as isAnon }
  def isBot               = me.exists(_.isBot)
  def noBot               = !isBot
  def isAppealUser        = me.exists(_.enabled.no)
  def ip                  = HTTPRequest ipAddress userContext.req
  val scopes: OAuthScopes = OAuthScopes(Nil)
  def isMobile            = scopes.has(_.Web.Mobile)
  def isWebAuth: Boolean

trait BodyContext[A] extends AnyContext:
  def body: Request[A]

/* Able to render a lichess page with a layout. Might be authenticated with cookie session */
class WebContext(
    val userContext: UserContext,
    val pageData: PageData
) extends AnyContext:

  export pageData.{ teamNbRequests, nbChallenges, nbNotifications, pref, blindMode as blind, nonce, hasClas }
  def isWebAuth = isAuth
  def noBlind   = !blind

  def currentTheme      = lila.pref.Theme(pref.theme)
  def currentTheme3d    = lila.pref.Theme3d(pref.theme3d)
  def currentPieceSet   = lila.pref.PieceSet.get(pref.pieceSet)
  def currentPieceSet3d = lila.pref.PieceSet3d.get(pref.pieceSet3d)
  def currentSoundSet   = lila.pref.SoundSet(pref.soundSet)

  lazy val currentBg =
    if pref.bg == Pref.Bg.TRANSPARENT then "transp"
    else if pref.bg == Pref.Bg.LIGHT then "light"
    else if pref.bg == Pref.Bg.SYSTEM then "system"
    else "dark" // dark && dark board

  lazy val mobileApiVersion = Mobile.Api requestVersion req

  def isMobileApi = mobileApiVersion.isDefined

  lazy val isMobileBrowser = HTTPRequest isMobile req

  def requiresFingerprint = isAuth && !pageData.hasFingerprint

  def zoom: Int = {
    def oldZoom = req.session get "zoom2" flatMap (_.toIntOption) map (_ - 100)
    req.cookies get "zoom" map (_.value) flatMap (_.toIntOption) orElse oldZoom filter (0 <=) filter (100 >=)
  } | 85

  def flash(name: String): Option[String] = req.flash get name

  def withLang(l: Lang) = new WebContext(userContext withLang l, pageData)

/* Able to render a lichess page with a layout. Might be authenticated with cookie session */
final class WebBodyContext[A](
    bodyContext: UserBodyContext[A],
    data: PageData
) extends WebContext(bodyContext, data)
    with BodyContext[A]:

  export bodyContext.body
  override def withLang(l: Lang) = WebBodyContext(bodyContext withLang l, data)

/* Cannot render a lichess page. Might be authenticated oauth and have scopes */
class OAuthContext(
    val userContext: UserContext,
    override val scopes: OAuthScopes
) extends AnyContext:
  def isWebAuth         = false
  def withLang(l: Lang) = OAuthContext(userContext withLang l, scopes)

final class OAuthBodyContext[A](
    val bodyContext: UserBodyContext[A],
    override val scopes: OAuthScopes
) extends OAuthContext(bodyContext, scopes)
    with BodyContext[A]:
  export bodyContext.body
  def scoped                     = me.map(OAuthScope.Scoped(_, scopes))
  override def withLang(l: Lang) = OAuthBodyContext(bodyContext withLang l, scopes)

/* Cannot render a lichess page. Cannot be authenticated. */
class MinimalContext(
    val userContext: UserContext
) extends AnyContext:
  def isWebAuth = false

final class MinimalBodyContext[A](
    val userContext: UserContext,
    val body: Request[A]
) extends BodyContext[A]:
  def isWebAuth = false

object WebContext:

  def error(req: RequestHeader, lang: Lang, nonce: Option[Nonce]): WebContext =
    WebContext(UserContext(req, none, none, lang), PageData.error(req, nonce))
