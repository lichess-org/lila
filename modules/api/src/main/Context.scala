package lila.api

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }

import lila.common.HTTPRequest
import lila.pref.Pref
import lila.user.{ BodyUserContext, HeaderUserContext, UserContext }
import lila.notify.Notification.UnreadCount
import lila.oauth.OAuthScope

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

trait AnyContext extends lila.user.UserContextWrapper:
  val userContext: UserContext
  export userContext.lang

trait HeaderContext extends AnyContext

trait BodyContext[A] extends AnyContext:
  val body: Request[A]

sealed trait WebContext extends AnyContext:
  val pageData: PageData

  def withLang(newLang: Lang): WebContext

  export pageData.{ teamNbRequests, nbChallenges, nbNotifications, pref, blindMode as blind, nonce, hasClas }
  def noBlind = !blind

  def currentTheme      = lila.pref.Theme(pref.theme)
  def currentTheme3d    = lila.pref.Theme3d(pref.theme3d)
  def currentPieceSet   = lila.pref.PieceSet.get(pref.pieceSet)
  def currentPieceSet3d = lila.pref.PieceSet3d.get(pref.pieceSet3d)
  def currentSoundSet   = lila.pref.SoundSet(pref.soundSet)

  lazy val currentBg =
    if (pref.bg == Pref.Bg.TRANSPARENT) "transp"
    else if (pref.bg == Pref.Bg.LIGHT) "light"
    else if (pref.bg == Pref.Bg.SYSTEM) "system"
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

sealed abstract class WebBaseContext(
    val userContext: lila.user.UserContext,
    val pageData: PageData
) extends WebContext

final class WebHeaderContext(
    headerContext: HeaderUserContext,
    data: PageData
) extends WebBaseContext(headerContext, data)
    with HeaderContext:
  def withLang(l: Lang) = WebHeaderContext(headerContext withLang l, data)

final class WebBodyContext[A](
    val bodyContext: BodyUserContext[A],
    data: PageData
) extends WebBaseContext(bodyContext, data)
    with BodyContext[A]:
  export bodyContext.body
  def withLang(l: Lang) = WebBodyContext(bodyContext withLang l, data)

sealed trait OAuthContext extends AnyContext:
  val scopes: List[OAuthScope]

sealed abstract class OAuthBaseContext(
    val userContext: lila.user.UserContext,
    val scopes: List[OAuthScope]
) extends OAuthContext

final class OAuthHeaderContext(
    headerContext: HeaderUserContext,
    scopes: List[OAuthScope]
) extends OAuthBaseContext(headerContext, scopes)
    with HeaderContext:
  def withLang(l: Lang) = OAuthHeaderContext(headerContext withLang l, scopes)

final class OAuthBodyContext[A](
    val bodyContext: BodyUserContext[A],
    scopes: List[OAuthScope]
) extends OAuthBaseContext(bodyContext, scopes)
    with BodyContext[A]:
  export bodyContext.body
  def scoped            = me.map(OAuthScope.Scoped(_, scopes))
  def withLang(l: Lang) = OAuthBodyContext(bodyContext withLang l, scopes)

object WebContext:

  def error(req: RequestHeader, lang: Lang, nonce: Option[Nonce]): WebHeaderContext =
    WebHeaderContext(UserContext(req, none, none, lang), PageData.error(req, nonce))

  def apply(userContext: HeaderUserContext, pageData: PageData): WebHeaderContext =
    WebHeaderContext(userContext, pageData)

  def apply[A](userContext: BodyUserContext[A], pageData: PageData): WebBodyContext[A] =
    WebBodyContext(userContext, pageData)

  def apply(userContext: HeaderUserContext, scopes: List[OAuthScope]): OAuthHeaderContext =
    OAuthHeaderContext(userContext, scopes)

  def apply[A](userContext: BodyUserContext[A], scopes: List[OAuthScope]): OAuthBodyContext[A] =
    OAuthBodyContext(userContext, scopes)
