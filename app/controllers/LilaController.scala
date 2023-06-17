package controllers

import alleycats.Zero
import play.api.data.Form
import play.api.data.FormBinding
import play.api.http.*
import play.api.i18n.Lang
import play.api.libs.json.{ JsArray, JsNumber, JsObject, JsString, JsValue, Json, Writes }
import play.api.mvc.*
import scalatags.Text.Frag

import lila.api.context.*
import lila.api.{ PageData, Nonce }
import lila.app.{ *, given }
import lila.common.{ ApiVersion, HTTPRequest, config }
import lila.i18n.{ I18nKey, I18nLangPicker }
import lila.oauth.{ OAuthScope, OAuthScopes, OAuthServer, EndpointScopes, TokenScopes }
import lila.security.{ AppealUser, FingerPrintedUser, Granter, Permission }
import lila.user.{ Holder, User, UserContext }

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with lila.app.http.RequestGetter
    with lila.app.http.ResponseBuilder(using env.executor)
    with lila.app.http.ResponseHeaders
    with lila.app.http.ResponseWriter
    with lila.app.http.CtrlExtensions
    with lila.app.http.CtrlConversions
    with lila.app.http.CtrlFilters
    with lila.app.http.RequestContext(using env.executor)
    with lila.app.http.CtrlErrors:

  def controllerComponents = env.controllerComponents
  given Executor           = env.executor
  given Scheduler          = env.scheduler

  given FormBinding = parse.formBinding(parse.DefaultMaxTextLength)

  given lila.common.config.NetDomain = env.net.domain

  inline def ctx(using it: WebContext)    = it // `ctx` is shorter and nicer than `summon[Context]`
  inline def req(using it: RequestHeader) = it // `req` is shorter and nicer than `summon[RequestHeader]`

  def reqLang(using req: RequestHeader): Lang = I18nLangPicker(req)

  /* Anonymous requests */
  def Anon(f: MinimalContext ?=> Fu[Result]): EssentialAction =
    action(parse.empty)(f(using minimalContext))

  /* Anonymous requests, with a body */
  def AnonBody(f: MinimalBodyContext[?] ?=> Fu[Result]): EssentialAction =
    action(parse.anyContent)(f(using minimalBodyContext))

  /* Anonymous requests, with a body */
  def AnonBodyOf[A](parser: BodyParser[A])(f: MinimalBodyContext[A] ?=> A => Fu[Result]): EssentialAction =
    action(parser)(req ?=> f(using minimalBodyContext)(req.body))

  /* Anonymous and authenticated requests */
  def Open(f: WebContext ?=> Fu[Result]): EssentialAction =
    OpenOf(parse.empty)(f)

  def OpenOf[A](parser: BodyParser[A])(f: WebContext ?=> Fu[Result]): EssentialAction =
    action(parser)(handleOpen(f))

  /* Anonymous and authenticated requests, with a body */
  def OpenBody(f: WebBodyContext[?] ?=> Fu[Result]): EssentialAction =
    OpenBodyOf(parse.anyContent)(f)

  /* Anonymous and authenticated requests, with a body */
  def OpenBodyOf[A](parser: BodyParser[A])(f: WebBodyContext[A] ?=> Fu[Result]): EssentialAction =
    action(parser)(handleOpenBody(f))

  private def handleOpenBody[A](f: WebBodyContext[A] ?=> Fu[Result])(using Request[A]): Fu[Result] =
    CSRF:
      webBodyContext.flatMap:
        f(using _)

  /* Anonymous, authenticated, and oauth requests */
  def OpenOrScoped(selectors: OAuthScope.Selector*)(
      open: WebContext ?=> Fu[Result],
      scoped: OAuthContext ?=> Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(_ ?=> _ => scoped)
      else handleOpen(open)

  /* Anonymous, authenticated, and oauth requests */
  def OpenOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Fu[Result]
  ): EssentialAction =
    OpenOrScoped(selectors*)(f, f)

  private def handleOpen(f: WebContext ?=> Fu[Result])(using RequestHeader): Fu[Result] =
    CSRF:
      webContext.flatMap:
        f(using _)

  /* Anonymous, authenticated, and oauth requests with a body */
  def OpenOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      f: BodyContext[A] ?=> Fu[Result]
  ): EssentialAction =
    action(parser): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScopedBody[A](selectors)(ctx ?=> _ => f(using ctx))
      else handleOpenBody(f)

  /* Anonymous and oauth requests */
  def AnonOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(_ ?=> _ => f)
      else f(using minimalContext)

  /* Anonymous and oauth requests with a body */
  def AnonOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      f: BodyContext[A] ?=> Fu[Result]
  ): EssentialAction =
    action(parser): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScopedBody[A](selectors)(_ ?=> _ => f)
      else f(using minimalBodyContext)

  /* Authenticated and oauth requests */
  def AuthOrScoped(selectors: OAuthScope.Selector*)(
      auth: WebContext ?=> User => Fu[Result],
      scoped: OAuthContext ?=> User => Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(scoped)
      else handleAuth(auth)

  def AuthOrScoped(
      selectors: OAuthScope.Selector*
  )(f: AnyContext ?=> User => Fu[Result]): EssentialAction =
    AuthOrScoped(selectors*)(auth = f, scoped = f)

  /* Authenticated and oauth requests with a body */
  def AuthOrScopedBody(selectors: OAuthScope.Selector*)(
      auth: WebBodyContext[?] ?=> User => Fu[Result],
      scoped: OAuthBodyContext[?] ?=> User => Fu[Result]
  ): EssentialAction =
    action(parse.anyContent): req ?=>
      if HTTPRequest.isOAuth(req)
      then handleScopedBody(selectors)(scoped)
      else handleAuthBody(auth)

  /* Authenticated requests */
  def Auth(f: WebContext ?=> User => Fu[Result]): EssentialAction =
    Auth(parse.empty)(f)

  /* Authenticated requests */
  def Auth[A](parser: BodyParser[A])(f: WebContext ?=> User => Fu[Result]): EssentialAction =
    action(parser)(handleAuth(f))

  private def handleAuth(f: WebContext ?=> User => Fu[Result])(using RequestHeader): Fu[Result] =
    CSRF:
      webContext.flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx))

  /* Authenticated requests with a body */
  def AuthBody(f: WebBodyContext[?] ?=> User => Fu[Result]): EssentialAction =
    AuthBody(parse.anyContent)(f)

  /* Authenticated requests with a body */
  def AuthBody[A](
      parser: BodyParser[A]
  )(f: WebBodyContext[A] ?=> User => Fu[Result]): EssentialAction =
    action(parser)(handleAuthBody(f))

  private def handleAuthBody[A](f: WebBodyContext[A] ?=> User => Fu[Result])(using Request[A]): Fu[Result] =
    CSRF:
      webBodyContext.flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx))

  /* Authenticated requests requiring certain permissions */
  def Secure(perm: Permission.Selector)(
      f: WebContext ?=> Holder => Fu[Result]
  ): EssentialAction =
    Secure(parse.anyContent)(perm)(f)

  /* Authenticated requests requiring certain permissions */
  def Secure[A](
      parser: BodyParser[A]
  )(perm: Permission.Selector)(f: WebContext ?=> Holder => Fu[Result]): EssentialAction =
    Auth(parser): me =>
      withSecure(me, perm)(f)

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody[A](
      parser: BodyParser[A]
  )(perm: Permission.Selector)(f: WebBodyContext[A] ?=> Holder => Fu[Result]): EssentialAction =
    AuthBody(parser): me =>
      withSecure(me, perm)(f)

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody(
      perm: Permission.Selector
  )(f: WebBodyContext[?] ?=> Holder => Fu[Result]): EssentialAction =
    SecureBody(parse.anyContent)(perm)(f)

  private def withSecure[C <: WebContext](me: User, perm: Permission.Selector)(
      f: C ?=> Holder => Fu[Result]
  )(using C) =
    if isGranted(perm, me)
    then f(Holder(me))
    else authorizationFailed

  /* OAuth requests */
  def Scoped[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthContext ?=> User => Fu[Result]): EssentialAction =
    action(parser)(handleScoped(selectors)(f))

  /* OAuth requests */
  def Scoped(
      selectors: OAuthScope.Selector*
  )(f: OAuthContext ?=> User => Fu[Result]): EssentialAction =
    Scoped(parse.empty)(selectors)(f)

  /* OAuth requests with a body */
  def ScopedBody[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthBodyContext[A] ?=> User => Fu[Result]): EssentialAction =
    action(parser)(handleScopedBody(selectors)(f))

  /* OAuth requests with a body */
  def ScopedBody(
      selectors: OAuthScope.Selector*
  )(f: OAuthBodyContext[?] ?=> User => Fu[Result]): EssentialAction =
    ScopedBody(parse.anyContent)(selectors)(f)

  private def handleScoped(
      selectors: Seq[OAuthScope.Selector]
  )(f: OAuthContext ?=> User => Fu[Result])(using RequestHeader): Fu[Result] =
    handleScopedCommon(selectors): scoped =>
      f(using oauthContext(scoped))(scoped.user)

  private def handleScopedBody[A](
      selectors: Seq[OAuthScope.Selector]
  )(f: OAuthBodyContext[A] ?=> User => Fu[Result])(using Request[A]): Fu[Result] =
    handleScopedCommon(selectors): scoped =>
      f(using oauthBodyContext(scoped))(scoped.user)

  private def handleScopedCommon(selectors: Seq[OAuthScope.Selector])(using req: RequestHeader)(
      f: OAuthScope.Scoped => Fu[Result]
  ) =
    val accepted = OAuthScope.select(selectors) into EndpointScopes
    env.security.api.oauthScoped(req, accepted).flatMap {
      case Left(e) =>
        monitorOauth(false)
        handleScopedFail(accepted, e)
      case Right(scoped) =>
        monitorOauth(true)
        f(scoped) map OAuthServer.responseHeaders(accepted, scoped.scopes)
    }

  def handleScopedFail(accepted: EndpointScopes, e: OAuthServer.AuthError)(using RequestHeader) = e match
    case e @ lila.oauth.OAuthServer.MissingScope(available) =>
      OAuthServer.responseHeaders(accepted, available):
        Forbidden(jsonError(e.message))
    case e =>
      OAuthServer.responseHeaders(accepted, TokenScopes(Nil)):
        Unauthorized(jsonError(e.message))

  private def monitorOauth(success: Boolean)(using req: RequestHeader) =
    lila.mon.user.oauth.request(HTTPRequest.userAgent(req).fold("none")(_.value), success).increment()

  /* Authenticated and OAuth requests requiring certain permissions */
  def SecuredScoped(perms: Permission.Selector)(
      f: OAuthContext ?=> Holder => Fu[Result]
  ): EssentialAction =
    Scoped() { _ ?=> me =>
      IfGranted(perms, me)(f(Holder(me)))
    }

  /* OAuth requests requiring certain permissions, with a body */
  def SecuredScopedBody(perm: Permission.Selector)(
      f: OAuthBodyContext[?] ?=> Holder => Fu[Result]
  ) =
    ScopedBody() { _ ?=> user =>
      IfGranted(perm)(f(Holder(user)))
    }

  /* Authenticated and OAuth requests requiring certain permissions */
  def SecureOrScoped(perm: Permission.Selector)(
      f: AnyContext ?=> Holder => Fu[Result]
  ): EssentialAction =
    action(parse.empty): req ?=>
      if HTTPRequest.isOAuth(req)
      then
        handleScoped(Seq.empty) { _ ?=> me =>
          IfGranted(perm, me)(f(Holder(me)))
        }
      else
        handleAuth { _ ?=> me =>
          withSecure(me, perm)(f)
        }

  /* Authenticated and OAuth requests requiring certain permissions, with a body */
  def SecureOrScopedBody(perm: Permission.Selector)(
      f: BodyContext[?] ?=> Holder => Fu[Result]
  ): EssentialAction =
    action(parse.anyContent): req ?=>
      if HTTPRequest.isOAuth(req)
      then
        handleScopedBody(Seq.empty) { _ ?=> me =>
          IfGranted(perm, me)(f(Holder(me)))
        }
      else
        handleAuthBody { _ ?=> me =>
          withSecure(me, perm)(f)
        }

  def FormFuResult[A, B: Writeable](
      form: Form[A]
  )(err: Form[A] => Fu[B])(op: A => Fu[Result])(using Request[?]) =
    form
      .bindFromRequest()
      .fold(
        form => err(form) dmap { BadRequest(_) },
        data => op(data)
      )

  def OptionOk[A, B: Writeable](
      fua: Fu[Option[A]]
  )(op: A => B)(using WebContext): Fu[Result] =
    OptionFuOk(fua): a =>
      fuccess(op(a))

  def OptionFuOk[A, B: Writeable](
      fua: Fu[Option[A]]
  )(op: A => Fu[B])(using WebContext) =
    fua flatMap { _.fold(notFound)(a => op(a) dmap { Ok(_) }) }

  def OptionFuRedirect[A](fua: Fu[Option[A]])(op: A => Fu[Call])(using WebContext): Fu[Result] =
    fua.flatMap:
      _.fold(notFound): a =>
        op(a).map:
          Redirect(_)

  def OptionFuRedirectUrl[A](fua: Fu[Option[A]])(op: A => Fu[String])(using WebContext): Fu[Result] =
    fua.flatMap:
      _.fold(notFound): a =>
        op(a).map:
          Redirect(_)

  def OptionResult[A](fua: Fu[Option[A]])(op: A => Result)(using WebContext): Fu[Result] =
    OptionFuResult(fua): a =>
      fuccess(op(a))

  def OptionFuResult[A](fua: Fu[Option[A]])(op: A => Fu[Result])(using AnyContext): Fu[Result] =
    fua flatMap { _.fold(notFound)(op) }

  def pageHit(using req: RequestHeader): Unit =
    if HTTPRequest.isHuman(req) then lila.mon.http.path(req.path).increment().unit

  def LangPage(call: Call)(f: WebContext ?=> Fu[Result])(langCode: String): EssentialAction =
    LangPage(call.url)(f)(langCode)
  def LangPage(path: String)(f: WebContext ?=> Fu[Result])(langCode: String): EssentialAction = Open:
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
          pageHit
          f(using ctx.withLang(lang))

  /* We roll our own action, as we don't want to compose play Actions. */
  private def action[A](parser: BodyParser[A])(handler: Request[A] ?=> Fu[Result]): EssentialAction = new:
    import play.api.libs.streams.Accumulator
    import akka.util.ByteString
    def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
      parser(rh).mapFuture:
        case Left(r)  => fuccess(r)
        case Right(a) => handler(using Request(rh, a))
