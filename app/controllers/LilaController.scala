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
  def Anon(f: MinimalContext ?=> Fu[Result]): Action[Unit] =
    Action.async(parse.empty)(req => f(using minimalContext(req)))

  /* Anonymous requests, with a body */
  def AnonBody(f: MinimalBodyContext[?] ?=> Fu[Result]): Action[AnyContent] =
    Action.async(parse.anyContent)(req => f(using minimalBodyContext(req)))

  /* Anonymous requests, with a body */
  def AnonBodyOf[A](parser: BodyParser[A])(f: MinimalBodyContext[A] ?=> A => Fu[Result]): Action[A] =
    Action.async(parser)(req => f(using minimalBodyContext(req))(req.body))

  /* Anonymous and authenticated requests */
  def Open(f: WebContext ?=> Fu[Result]): Action[Unit] =
    OpenOf(parse.empty)(f)

  def OpenOf[A](parser: BodyParser[A])(f: WebContext ?=> Fu[Result]): Action[A] =
    Action.async(parser)(handleOpen(f, _))

  /* Anonymous and authenticated requests, with a body */
  def OpenBody(f: WebBodyContext[?] ?=> Fu[Result]): Action[AnyContent] =
    OpenBodyOf(parse.anyContent)(f)

  /* Anonymous and authenticated requests, with a body */
  def OpenBodyOf[A](parser: BodyParser[A])(f: WebBodyContext[A] ?=> Fu[Result]): Action[A] =
    Action.async(parser): req =>
      CSRF(req):
        webBodyContext(req).flatMap:
          f(using _)

  /* Anonymous, authenticated, and oauth requests */
  def OpenOrScoped(selectors: OAuthScope.Selector*)(
      open: WebContext ?=> Fu[Result],
      scoped: OAuthContext ?=> Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(_ ?=> _ => scoped)(using req)
      else handleOpen(open, req)

  /* Anonymous, authenticated, and oauth requests */
  def OpenOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Fu[Result]
  ): Action[Unit] =
    OpenOrScoped(selectors*)(f, f)

  private def handleOpen(f: WebContext ?=> Fu[Result], req: RequestHeader): Fu[Result] =
    CSRF(req):
      webContext(req).flatMap:
        f(using _)

  // def OpenOrScopedBody(selectors: OAuthScope.Selector*)(
  //     open: BodyContext[_] => Fu[Result],
  //     scoped: Request[_] => User => Fu[Result]
  // ): Action[AnyContent] = OpenOrScopedBody(parse.anyContent)(selectors)(auth, scoped)

  /* Anonymous, authenticated, and oauth requests with a body */
  def OpenOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      f: BodyContext[A] ?=> Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(ctx ?=> _ => f(using ctx))(req)
      else OpenBodyOf(parser)(f)(req)

  /* Anonymous and oauth requests */
  def AnonOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(ctx ?=> _ => f(using ctx))(using req)
      else f(using minimalContext(req))

  /* Anonymous and oauth requests with a body */
  def AnonOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      f: BodyContext[A] ?=> Option[User] => Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(ctx ?=> user => f(using ctx)(user.some))(req)
      else f(using minimalBodyContext(req))(none)

  /* Authenticated and oauth requests */
  def AuthOrScoped(selectors: OAuthScope.Selector*)(
      auth: WebContext ?=> User => Fu[Result],
      scoped: OAuthContext ?=> User => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(scoped)(using req)
      else handleAuth(auth, req)

  def AuthOrScoped(
      selectors: OAuthScope.Selector*
  )(f: AnyContext ?=> User => Fu[Result]): Action[Unit] =
    AuthOrScoped(selectors*)(auth = f, scoped = f)

  /* Authenticated and oauth requests with a body */
  def AuthOrScopedBody(selectors: OAuthScope.Selector*)(
      auth: WebBodyContext[?] ?=> User => Fu[Result],
      scoped: OAuthBodyContext[?] ?=> User => Fu[Result]
  ): Action[AnyContent] = AuthOrScopedBody(parse.anyContent)(selectors)(auth, scoped)

  /* Authenticated and oauth requests with a body */
  def AuthOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      auth: WebBodyContext[A] ?=> User => Fu[Result],
      scoped: OAuthBodyContext[A] ?=> User => Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(scoped)(req)
      else AuthBody(parser)(auth)(req)

  /* Authenticated requests */
  def Auth(f: WebContext ?=> User => Fu[Result]): Action[Unit] =
    Auth(parse.empty)(f)

  /* Authenticated requests */
  def Auth[A](parser: BodyParser[A])(f: WebContext ?=> User => Fu[Result]): Action[A] =
    Action.async(parser) { handleAuth(f, _) }

  private def handleAuth(f: WebContext ?=> User => Fu[Result], req: RequestHeader): Fu[Result] =
    CSRF(req):
      webContext(req).flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx))

  /* Authenticated requests with a body */
  def AuthBody(f: WebBodyContext[?] ?=> User => Fu[Result]): Action[AnyContent] =
    AuthBody(parse.anyContent)(f)

  /* Authenticated requests with a body */
  def AuthBody[A](
      parser: BodyParser[A]
  )(f: WebBodyContext[A] ?=> User => Fu[Result]): Action[A] =
    Action.async(parser): req =>
      CSRF(req):
        webBodyContext(req).flatMap: ctx =>
          ctx.me.fold(authenticationFailed(using ctx))(f(using ctx))

  /* Authenticated requests requiring certain permissions */
  def Secure(perm: Permission.Selector)(
      f: WebContext ?=> Holder => Fu[Result]
  ): Action[AnyContent] =
    Secure(parse.anyContent)(perm(Permission))(f)

  /* Authenticated requests requiring certain permissions */
  def Secure[A](
      parser: BodyParser[A]
  )(perm: Permission)(f: WebContext ?=> Holder => Fu[Result]): Action[A] =
    Auth(parser): me =>
      if isGranted(perm)
      then f(Holder(me))
      else authorizationFailed

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody[A](
      parser: BodyParser[A]
  )(perm: Permission)(f: WebBodyContext[A] ?=> Holder => Fu[Result]): Action[A] =
    AuthBody(parser) { ctx ?=> me =>
      if isGranted(perm)
      then f(Holder(me))
      else authorizationFailed
    }

  /* Authenticated requests requiring certain permissions, with a body */
  def SecureBody(
      perm: Permission.Selector
  )(f: WebBodyContext[?] ?=> Holder => Fu[Result]): Action[AnyContent] =
    SecureBody(parse.anyContent)(perm(Permission))(f)

  /* OAuth requests */
  def Scoped[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthContext ?=> User => Fu[Result]): Action[A] =
    Action.async(parser)(handleScoped(selectors)(f)(using _))

  /* OAuth requests */
  def Scoped(
      selectors: OAuthScope.Selector*
  )(f: OAuthContext ?=> User => Fu[Result]): Action[Unit] =
    Scoped(parse.empty)(selectors)(f)

  /* OAuth requests with a body */
  def ScopedBody[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthBodyContext[A] ?=> User => Fu[Result]): Action[A] =
    Action.async(parser)(handleScopedBody(selectors)(f)(using _))

  /* OAuth requests with a body */
  def ScopedBody(
      selectors: OAuthScope.Selector*
  )(f: OAuthBodyContext[?] ?=> User => Fu[Result]): Action[AnyContent] =
    ScopedBody(parse.anyContent)(selectors)(f)

  private def handleScoped(
      selectors: Seq[OAuthScope.Selector]
  )(f: OAuthContext ?=> User => Fu[Result])(using req: RequestHeader): Fu[Result] =
    handleScopedCommon(selectors): scoped =>
      f(using oauthContext(scoped))(scoped.user)

  private def handleScopedBody[A](
      selectors: Seq[OAuthScope.Selector]
  )(f: OAuthBodyContext[A] ?=> User => Fu[Result])(using req: Request[A]): Fu[Result] =
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
  def SecureOrScoped(perm: Permission.Selector)(
      secure: WebContext ?=> Holder => Fu[Result],
      scoped: OAuthContext ?=> Holder => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then SecuredScoped(perm)(scoped)(req)
      else Secure(parse.empty)(perm(Permission))(secure)(req)

  /* Authenticated and OAuth requests requiring certain permissions */
  def SecuredScoped(perm: Permission.Selector)(
      f: OAuthContext ?=> Holder => Fu[Result]
  ) =
    Scoped() { ctx ?=> me =>
      IfGranted(perm, me)(f(Holder(me)))
    }

  /* Authenticated and OAuth requests requiring certain permissions, with a body */
  def SecureOrScopedBody(perm: Permission.Selector)(
      secure: WebBodyContext[?] ?=> Holder => Fu[Result],
      scoped: OAuthBodyContext[?] ?=> Holder => Fu[Result]
  ): Action[AnyContent] =
    Action.async(parse.anyContent): req =>
      if HTTPRequest.isOAuth(req)
      then SecuredScopedBody(perm)(scoped)(req)
      else SecureBody(parse.anyContent)(perm(Permission))(secure)(req)

  /* OAuth requests requiring certain permissions, with a body */
  def SecuredScopedBody(perm: Permission.Selector)(
      f: OAuthBodyContext[?] ?=> Holder => Fu[Result]
  ) =
    ScopedBody() { ctx ?=> user =>
      IfGranted(perm)(f(Holder(user)))
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
  )(op: A => Fu[B])(using ctx: WebContext) =
    fua flatMap { _.fold(notFound(using ctx))(a => op(a) dmap { Ok(_) }) }

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

  def LangPage(call: Call)(f: WebContext ?=> Fu[Result])(langCode: String): Action[Unit] =
    LangPage(call.url)(f)(langCode)
  def LangPage(path: String)(f: WebContext ?=> Fu[Result])(langCode: String): Action[Unit] = Open:
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
