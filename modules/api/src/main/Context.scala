package lidraughts.api

import play.api.mvc.RequestHeader

import lidraughts.common.{ HTTPRequest, Nonce, Lang }
import lidraughts.pref.Pref
import lidraughts.relation.actorApi.OnlineFriends
import lidraughts.user.{ UserContext, HeaderUserContext, BodyUserContext }

case class PageData(
    onlineFriends: OnlineFriends,
    teamNbRequests: Int,
    nbChallenges: Int,
    nbNotifications: Int,
    pref: Pref,
    blindMode: Boolean,
    hasFingerprint: Boolean,
    inquiry: Option[lidraughts.mod.Inquiry],
    nonce: Option[Nonce],
    error: Boolean = false
)

object PageData {

  def anon(req: RequestHeader, nonce: Option[Nonce], blindMode: Boolean = false) = PageData(
    OnlineFriends.empty,
    teamNbRequests = 0,
    nbChallenges = 0,
    nbNotifications = 0,
    lidraughts.pref.RequestPref fromRequest req,
    blindMode = blindMode,
    hasFingerprint = false,
    inquiry = none,
    nonce = nonce
  )

  def error(req: RequestHeader, nonce: Option[Nonce]) = anon(req, nonce).copy(error = true)
}

sealed trait Context extends lidraughts.user.UserContextWrapper {

  val userContext: UserContext
  val pageData: PageData

  def lang = userContext.lang

  def onlineFriends = pageData.onlineFriends

  def teamNbRequests = pageData.teamNbRequests
  def nbChallenges = pageData.nbChallenges
  def nbNotifications = pageData.nbNotifications
  def pref = pageData.pref
  def blind = pageData.blindMode
  def noBlind = !blind
  def nonce = pageData.nonce

  def currentTheme = lidraughts.pref.Theme(pref.theme)

  def currentPieceSet = lidraughts.pref.PieceSet(pref.pieceSet)

  def currentSoundSet = lidraughts.pref.SoundSet(pref.soundSet)

  lazy val currentBg = if (pref.transp) "transp" else if (pref.dark) "dark" else "light"

  def transpBgImg = currentBg == "transp" option pref.bgImgOrDefault

  lazy val mobileApiVersion = Mobile.Api requestVersion req

  def isMobileApi = mobileApiVersion.isDefined

  lazy val isMobileBrowser = HTTPRequest isMobile req

  def requiresFingerprint = isAuth && !pageData.hasFingerprint

  def zoom: Option[Int] = req.session get "zoom" flatMap parseIntOption filter (100<=)

  def respZoom = zoom.fold(85)(_ - 100)
}

sealed abstract class BaseContext(
    val userContext: lidraughts.user.UserContext,
    val pageData: PageData
) extends Context

final class BodyContext[A](
    val bodyContext: BodyUserContext[A],
    data: PageData
) extends BaseContext(bodyContext, data) {

  def body = bodyContext.body
}

final class HeaderContext(
    headerContext: HeaderUserContext,
    data: PageData
) extends BaseContext(headerContext, data)

object Context {

  def error(req: RequestHeader, lang: Lang, nonce: Option[Nonce]): HeaderContext =
    new HeaderContext(UserContext(req, none, none, lang), PageData.error(req, nonce))

  def apply(userContext: HeaderUserContext, pageData: PageData): HeaderContext =
    new HeaderContext(userContext, pageData)

  def apply[A](userContext: BodyUserContext[A], pageData: PageData): BodyContext[A] =
    new BodyContext(userContext, pageData)
}
