package lila.api

import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.pref.Pref
import lila.user.{ BodyUserContext, HeaderUserContext, UserContext }
import lila.notify.Notification.UnreadCount

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

sealed trait Context extends lila.user.UserContextWrapper:

  val userContext: UserContext
  val pageData: PageData

  def withLang(newLang: Lang): Context

  def lang = userContext.lang

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

sealed abstract class BaseContext(
    val userContext: lila.user.UserContext,
    val pageData: PageData
) extends Context

final class BodyContext[A](
    val bodyContext: BodyUserContext[A],
    data: PageData
) extends BaseContext(bodyContext, data):

  def body = bodyContext.body

  def withLang(l: Lang) = new BodyContext(bodyContext withLang l, data)

final class HeaderContext(
    headerContext: HeaderUserContext,
    data: PageData
) extends BaseContext(headerContext, data):

  def withLang(l: Lang) = new HeaderContext(headerContext withLang l, data)

object Context:

  def error(req: RequestHeader, lang: Lang, nonce: Option[Nonce]): HeaderContext =
    new HeaderContext(UserContext(req, none, none, lang), PageData.error(req, nonce))

  def apply(userContext: HeaderUserContext, pageData: PageData): HeaderContext =
    new HeaderContext(userContext, pageData)

  def apply[A](userContext: BodyUserContext[A], pageData: PageData): BodyContext[A] =
    new BodyContext(userContext, pageData)
