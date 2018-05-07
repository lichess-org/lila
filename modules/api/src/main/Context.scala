package lila.api

import play.api.mvc.RequestHeader
import play.api.i18n.Lang

import lila.common.{ HTTPRequest, AssetVersion, Nonce }
import lila.pref.Pref
import lila.relation.actorApi.OnlineFriends
import lila.user.{ UserContext, HeaderUserContext, BodyUserContext }

case class PageData(
    onlineFriends: OnlineFriends,
    teamNbRequests: Int,
    nbChallenges: Int,
    nbNotifications: Int,
    pref: Pref,
    blindMode: Boolean,
    hasFingerprint: Boolean,
    assetVersion: AssetVersion,
    inquiry: Option[lila.mod.Inquiry],
    error: Boolean = false
)

object PageData {

  def empty(v: AssetVersion) =
    PageData(OnlineFriends.empty, 0, 0, 0, Pref.default, false, false, v, none)

  def anon(req: RequestHeader, v: AssetVersion, blindMode: Boolean = false) = PageData(
    OnlineFriends.empty,
    teamNbRequests = 0,
    nbChallenges = 0,
    nbNotifications = 0,
    lila.pref.RequestPref fromRequest req,
    blindMode = blindMode,
    hasFingerprint = false,
    assetVersion = v,
    none
  )

  def error(req: RequestHeader, v: AssetVersion) = anon(req, v).copy(error = true)
}

sealed trait Context extends lila.user.UserContextWrapper {

  val userContext: UserContext
  val pageData: PageData
  val nonce: Nonce

  def lang = userContext.lang

  def onlineFriends = pageData.onlineFriends

  def teamNbRequests = pageData.teamNbRequests
  def nbChallenges = pageData.nbChallenges
  def nbNotifications = pageData.nbNotifications
  def pref = pageData.pref
  def blindMode = pageData.blindMode

  def currentTheme = lila.pref.Theme(pref.theme)

  def currentTheme3d = lila.pref.Theme3d(pref.theme3d)

  def currentPieceSet = lila.pref.PieceSet(pref.pieceSet)

  def currentPieceSet3d = lila.pref.PieceSet3d(pref.pieceSet3d)

  def currentSoundSet = lila.pref.SoundSet(pref.soundSet)

  lazy val currentBg = if (pref.transp) "transp" else if (pref.dark) "dark" else "light"

  def transpBgImg = currentBg == "transp" option pref.bgImgOrDefault

  lazy val mobileApiVersion = Mobile.Api requestVersion req

  def isMobileApi = mobileApiVersion.isDefined

  lazy val isMobileBrowser = HTTPRequest isMobile req

  def requiresFingerprint = isAuth && !pageData.hasFingerprint

  def zoom: Option[Int] = req.session get "zoom" flatMap parseIntOption filter (100<)
}

sealed abstract class BaseContext(
    val userContext: lila.user.UserContext,
    val pageData: PageData,
    val nonce: Nonce
) extends Context

final class BodyContext[A](
    val bodyContext: BodyUserContext[A],
    data: PageData,
    nonce: Nonce
) extends BaseContext(bodyContext, data, nonce) {

  def body = bodyContext.body
}

final class HeaderContext(
    headerContext: HeaderUserContext,
    data: PageData,
    nonce: Nonce
) extends BaseContext(headerContext, data, nonce)

object Context {

  def error(req: RequestHeader, v: AssetVersion, lang: Lang): HeaderContext =
    new HeaderContext(UserContext(req, none, none, lang), PageData.error(req, v), Nonce.random)

  def apply(userContext: HeaderUserContext, pageData: PageData): HeaderContext =
    new HeaderContext(userContext, pageData, Nonce.random)

  def apply[A](userContext: BodyUserContext[A], pageData: PageData): BodyContext[A] =
    new BodyContext(userContext, pageData, Nonce.random)
}
