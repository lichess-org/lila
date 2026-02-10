package lila.api

import play.api.i18n.Lang
import play.api.mvc.{ Request, RequestHeader }
import alleycats.Zero

import lila.common.HTTPRequest
import lila.core.i18n.Translate
import lila.core.net.IpAddress
import lila.core.notify.UnreadCount
import lila.core.user.KidMode
import lila.oauth.TokenScopes
import lila.pref.Pref
import lila.ui.Nonce

/* Who is logged in, and how */
final class LoginContext(
    val me: Option[Me],
    val needsFp: Boolean,
    val impersonatedBy: Option[lila.core.userId.ModId],
    val oauth: Option[TokenScopes]
):
  export me.{ isDefined as isAuth, isEmpty as isAnon }
  def user: Option[User] = Me.raw(me)
  def userId: Option[UserId] = user.map(_.id)
  def username: Option[UserName] = user.map(_.username)
  def isBot = me.exists(_.isBot)
  def troll = user.exists(_.marks.troll)
  def isAppealUser = me.exists(_.enabled.no)
  def isWebAuth = isAuth && oauth.isEmpty
  def isOAuth = isAuth && oauth.isDefined
  def isMobileOauth = oauth.exists(_.has(_.Web.Mobile))
  def scopes = oauth | TokenScopes(Nil)
  def useMe[A: Zero](f: Me ?=> A): A = me.soUse(f)

object LoginContext:
  val anon = LoginContext(none, false, none, none)

/* Data available in every HTTP request */
class Context(
    val req: RequestHeader,
    val lang: Lang,
    val loginContext: LoginContext,
    val pref: Pref
) extends lila.ui.Context:
  export loginContext.*
  def ip: IpAddress = HTTPRequest.ipAddress(req)
  lazy val mobileApiVersion = lila.security.Mobile.Api.requestVersion(req)
  lazy val blind = req.cookies.get(lila.web.WebConfig.blindCookie.name).exists(_.value.nonEmpty)
  def isMobileApi = mobileApiVersion.isDefined
  def kid = KidMode(HTTPRequest.isKid(req) || loginContext.user.exists(_.kid.yes))
  def withLang(l: Lang) = new Context(req, l, loginContext, pref)
  def updatePref(f: Update[Pref]) = new Context(req, lang, loginContext, f(pref))
  def canVoiceChat = kid.no && me.exists(!_.marks.troll)
  lazy val translate = Translate(lila.i18n.Translator, lang)

object Context:
  export lila.api.{ Context, BodyContext, LoginContext, PageContext, EmbedContext }
  given (using ctx: Context): Option[Me] = ctx.me
  given (using ctx: Context): Option[MyId] = ctx.myId
  given (using ctx: Context): KidMode = ctx.kid
  given ctxToTranslate(using ctx: Context): Translate = ctx.translate
  given (using page: PageContext): Context = page.ctx
  given (using embed: EmbedContext): Context = embed.ctx

  import lila.i18n.LangPicker
  import lila.pref.RequestPref
  def minimal(req: RequestHeader) =
    Context(req, LangPicker(req), LoginContext.anon, RequestPref.fromRequest(req))
  def minimalBody[A](req: Request[A]) =
    BodyContext(req, LangPicker(req), LoginContext.anon, RequestPref.fromRequest(req))

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
  def anon(nonce: Option[Nonce]) = PageData(0, 0, UnreadCount(0), false, none, nonce)
  def error(nonce: Option[Nonce]) = anon(nonce).copy(error = true)

final class PageContext(val ctx: Context, val data: PageData) extends lila.ui.PageContext:
  export ctx.*
  export data.*
  def hasInquiry = inquiry.isDefined

final class EmbedContext(val ctx: Context, val bg: String, val nonce: Nonce):
  export ctx.*
  def boardClass = ctx.pref.realTheme.name
  def pieceSet = ctx.pref.realPieceSet

object EmbedContext:
  given (using config: EmbedContext): Lang = config.lang
  def apply(ctx: Context): EmbedContext = new EmbedContext(
    ctx,
    bg = HTTPRequest.queryStringGet("bg")(using ctx.req).filterNot("auto".==) | "system",
    nonce = Nonce.random
  )
