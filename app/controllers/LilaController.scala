package controllers

import play.api.data.Form
import play.api.data.FormBinding
import play.api.http.*
import play.api.i18n.Lang
import play.api.libs.json.{ JsArray, JsNumber, JsObject, JsString, JsValue, Json, Writes }
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.{ ApiVersion, HTTPRequest, config }
import lila.i18n.{ I18nKey, I18nLangPicker }
import lila.oauth.{ OAuthScope, OAuthScopes, OAuthServer, EndpointScopes, TokenScopes }
import lila.security.{ AppealUser, FingerPrintedUser, Granter, Permission }

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with http.RequestGetter
    with http.ResponseBuilder(using env.executor)
    with http.ResponseHeaders
    with http.ResponseWriter
    with http.CtrlExtensions
    with http.CtrlConversions
    with http.CtrlFilters
    with http.CtrlPage(using env.executor)
    with http.RequestContext(using env.executor)
    with http.CtrlErrors:

  def controllerComponents = env.controllerComponents
  given Executor           = env.executor
  given Scheduler          = env.scheduler
  given FormBinding        = parse.formBinding(parse.DefaultMaxTextLength)

  given lila.common.config.NetDomain = env.net.domain

  inline def ctx(using it: Context)       = it // `ctx` is shorter and nicer than `summon[Context]`
  inline def req(using it: RequestHeader) = it // `req` is shorter and nicer than `summon[RequestHeader]`

  /* Anonymous requests */
  def Anon(f: Context ?=> Fu[Result]): EssentialAction =
    action(parse.empty)(req ?=> f(using Context.minimal(req)))

  /* Anonymous requests, with a body */
  def AnonBody(f: BodyContext[?] ?=> Fu[Result]): EssentialAction =
    action(parse.anyContent)(req ?=> f(using Context.minimalBody(req)))

  /* Anonymous requests, with a body */
  def AnonBodyOf[A](parser: BodyParser[A])(f: BodyContext[A] ?=> A => Fu[Result]): EssentialAction =
    action(parser)(req ?=> f(using Context.minimalBody(req))(req.body))

  /* Anonymous and authenticated requests */
  def Open(f: Context ?=> Fu[Result]): EssentialAction =
    OpenOf(parse.empty)(f)

  def OpenOf[A](parser: BodyParser[A])(f: Context ?=> Fu[Result]): EssentialAction =
    action(parser)(handleOpen(f))

  /* Anonymous and authenticated requests, with a body */
  def OpenBody(f: BodyContext[?] ?=> Fu[Result]): EssentialAction =
    OpenBodyOf(parse.anyContent)(f)

  /* Anonymous and authenticated requests, with a body */
  def OpenBodyOf[A](parser: BodyParser[A])(f: BodyContext[A] ?=> Fu[Result]): EssentialAction =
    action(parser)(handleOpenBody(f))

  private def handleOpenBody[A](f: BodyContext[A] ?=> Fu[Result])(using Request[A]): Fu[Result] =
    CSRF:
      makeBodyContext.flatMap:
        f(using _)

  /* Anonymous, authenticated, and oauth requests */
  def OpenOrScoped(selectors: OAuthScope.Selector*)(
      f: Context ?=> Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(_ ?=> _ ?=> f)
      else handleOpen(f)

  private def handleOpen(f: Context ?=> Fu[Result])(using RequestHeader): Fu[Result] =
    CSRF:
      makeContext.flatMap:
        f(using _)

  /* Anonymous, authenticated, and oauth requests with a body */
  def OpenOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      f: BodyContext[A] ?=> Fu[Result]
  ): EssentialAction =
    action(parser): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScopedBody[A](selectors)(ctx ?=> _ ?=> f(using ctx))
      else handleOpenBody(f)

  /* Anonymous and oauth requests */
  def AnonOrScoped(selectors: OAuthScope.Selector*)(
      f: Context ?=> Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(f)
      else f(using Context.minimal(req))

  /* Anonymous and oauth requests with a body */
  def AnonOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      f: BodyContext[A] ?=> Fu[Result]
  ): EssentialAction =
    action(parser): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScopedBody[A](selectors)(f)
      else f(using Context.minimalBody(req))

  /* Authenticated and oauth requests */
  def AuthOrScoped(selectors: OAuthScope.Selector*)(
      f: Context ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(f)
      else handleAuth(f)

  /* Authenticated and oauth requests with a body */
  def AuthOrScopedBody(selectors: OAuthScope.Selector*)(
      f: BodyContext[?] ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    action(parse.anyContent): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScopedBody(selectors)(f)
      else handleAuthBody(f)

  /* Authenticated requests */
  def Auth(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    Auth(parse.empty)(f)

  /* Authenticated requests */
  def Auth[A](parser: BodyParser[A])(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser)(handleAuth(f))

  private def handleAuth(f: Context ?=> Me ?=> Fu[Result])(using RequestHeader): Fu[Result] =
    CSRF:
      makeContext.flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx)(using _))

  /* Authenticated requests with a body */
  def AuthBody(f: BodyContext[?] ?=> Me ?=> Fu[Result]): EssentialAction =
    AuthBody(parse.anyContent)(f)

  /* Authenticated requests with a body */
  def AuthBody[A](
      parser: BodyParser[A]
  )(f: BodyContext[A] ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser)(handleAuthBody(f))

  private def handleAuthBody[A](f: BodyContext[A] ?=> Me ?=> Fu[Result])(using Request[A]): Fu[Result] =
    CSRF:
      makeBodyContext.flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx)(using _))

  /* Authenticated requests requiring certain permissions */
  def Secure(perm: Permission.Selector)(
      f: Context ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    Secure(parse.anyContent)(perm)(f)

  /* Authenticated requests requiring certain permissions */
  def Secure[A](
      parser: BodyParser[A]
  )(perm: Permission.Selector)(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    Auth(parser): me ?=>
      withSecure(perm)(f)

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody[A](
      parser: BodyParser[A]
  )(perm: Permission.Selector)(f: BodyContext[A] ?=> Me ?=> Fu[Result]): EssentialAction =
    AuthBody(parser): me ?=>
      withSecure(perm)(f)

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody(
      perm: Permission.Selector
  )(f: BodyContext[?] ?=> Me ?=> Fu[Result]): EssentialAction =
    SecureBody(parse.anyContent)(perm)(f)

  private def withSecure[C <: Context](perm: Permission.Selector)(
      f: C ?=> Me ?=> Fu[Result]
  )(using C, Me) =
    if isGranted(perm)
    then f
    else authorizationFailed

  /* OAuth requests */
  def Scoped[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser)(handleScoped(selectors)(f))

  /* OAuth requests */
  def Scoped(
      selectors: OAuthScope.Selector*
  )(f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    Scoped(parse.empty)(selectors)(f)

  /* OAuth requests with a body */
  def ScopedBody[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: BodyContext[A] ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser)(handleScopedBody(selectors)(f))

  /* OAuth requests with a body */
  def ScopedBody(
      selectors: OAuthScope.Selector*
  )(f: BodyContext[?] ?=> Me ?=> Fu[Result]): EssentialAction =
    ScopedBody(parse.anyContent)(selectors)(f)

  private def handleScoped(
      selectors: Seq[OAuthScope.Selector]
  )(f: Context ?=> Me ?=> Fu[Result])(using RequestHeader): Fu[Result] =
    handleScopedCommon(selectors): scoped =>
      oauthContext(scoped).flatMap: ctx =>
        f(using ctx)(using scoped.me)

  private def handleScopedBody[A](
      selectors: Seq[OAuthScope.Selector]
  )(f: BodyContext[A] ?=> Me ?=> Fu[Result])(using Request[A]): Fu[Result] =
    handleScopedCommon(selectors): scoped =>
      oauthBodyContext(scoped).flatMap: ctx =>
        f(using ctx)(using scoped.me)

  private def handleScopedCommon(selectors: Seq[OAuthScope.Selector])(using req: RequestHeader)(
      f: OAuthScope.Scoped => Fu[Result]
  ) =
    val accepted = OAuthScope.select(selectors) into EndpointScopes
    env.security.api.oauthScoped(req, accepted).flatMap {
      case Left(e)       => handleScopedFail(accepted, e)
      case Right(scoped) => f(scoped) map OAuthServer.responseHeaders(accepted, scoped.scopes)
    }

  def handleScopedFail(accepted: EndpointScopes, e: OAuthServer.AuthError)(using RequestHeader) = e match
    case e @ lila.oauth.OAuthServer.MissingScope(available) =>
      OAuthServer.responseHeaders(accepted, available):
        Forbidden(jsonError(e.message))
    case e =>
      OAuthServer.responseHeaders(accepted, TokenScopes(Nil)):
        Unauthorized(jsonError(e.message))

  /* Authenticated and OAuth requests requiring certain permissions */
  def SecuredScoped(perms: Permission.Selector)(
      f: Context ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    Scoped() { _ ?=> _ ?=>
      IfGranted(perms)(f)
    }

  /* OAuth requests requiring certain permissions, with a body */
  def SecuredScopedBody(perm: Permission.Selector)(
      f: BodyContext[?] ?=> Me ?=> Fu[Result]
  ) =
    ScopedBody() { _ ?=> _ ?=>
      IfGranted(perm)(f)
    }

  /* Authenticated and OAuth requests requiring certain permissions */
  def SecureOrScoped(perm: Permission.Selector)(
      f: Context ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then
        handleScoped(Seq.empty) { _ ?=> _ ?=>
          IfGranted(perm)(f)
        }
      else
        handleAuth { _ ?=> _ ?=>
          withSecure(perm)(f)
        }

  /* Authenticated and OAuth requests requiring certain permissions, with a body */
  def SecureOrScopedBody(perm: Permission.Selector)(
      f: BodyContext[?] ?=> Me ?=> Fu[Result]
  ): EssentialAction =
    action(parse.anyContent): req ?=>
      if HTTPRequest.isOAuth(req)
      then
        handleScopedBody(Seq.empty) { _ ?=> _ ?=>
          IfGranted(perm)(f)
        }
      else
        handleAuthBody { _ ?=> _ ?=>
          withSecure(perm)(f)
        }

  def FormFuResult[A, B: Writeable](
      form: Form[A]
  )(err: Form[A] => Fu[B])(op: A => Fu[Result])(using Request[?]): Fu[Result] =
    form
      .bindFromRequest()
      .fold(
        form => err(form) dmap { BadRequest(_) },
        op
      )

  def HeadLastModifiedAt(updatedAt: Instant)(f: => Fu[Result])(using RequestHeader): Fu[Result] =
    if req.method == "HEAD" then NoContent.withDateHeaders(lastModified(updatedAt))
    else f

  def pageHit(using req: RequestHeader): Unit =
    if HTTPRequest.isHuman(req) then lila.mon.http.path(req.path).increment()

  def LangPage(call: Call)(f: Context ?=> Fu[Result])(langCode: String): EssentialAction =
    LangPage(call.url)(f)(langCode)
  def LangPage(path: String)(f: Context ?=> Fu[Result])(langCode: String): EssentialAction = Open:
    if ctx.isAuth
    then redirectWithQueryString(path)
    else
      import I18nLangPicker.ByHref
      I18nLangPicker.byHref(langCode, ctx.req) match
        case ByHref.NotFound => notFound(using ctx)
        case ByHref.Redir(code) =>
          redirectWithQueryString(s"/$code${~path.some.filter("/" !=)}")
        case ByHref.Refused(_) => redirectWithQueryString(path)
        case ByHref.Found(lang) =>
          f(using ctx.withLang(lang))

  import lila.rating.{ Perf, PerfType }
  def WithMyPerf[A](pt: PerfType)(f: Perf ?=> Fu[A])(using me: Option[Me]): Fu[A] = me
    .soFu(env.user.perfsRepo.perfOf(_, pt))
    .flatMap: perf =>
      f(using perf | Perf.default)
  def WithMyPerfs[A](f: Option[lila.user.User.WithPerfs] ?=> Fu[A])(using me: Option[Me]): Fu[A] = me
    .soFu(me => env.user.api.withPerfs(me.value))
    .flatMap:
      f(using _)

  /* We roll our own action, as we don't want to compose play Actions. */
  private def action[A](parser: BodyParser[A])(handler: Request[A] ?=> Fu[Result]): EssentialAction = new:
    import play.api.libs.streams.Accumulator
    import akka.util.ByteString
    def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
      parser(rh).mapFuture:
        case Left(r)  => fuccess(r)
        case Right(a) => handler(using Request(rh, a))
