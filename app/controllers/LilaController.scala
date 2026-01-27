package controllers

import cats.mtl.Handle.*
import play.api.data.{ Form, FormBinding }
import play.api.http.*
import play.api.libs.json.Writes
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import scalalib.model.Language
import lila.core.perf.UserWithPerfs
import lila.core.perm.Permission
import lila.i18n.LangPicker
import lila.oauth.{ EndpointScopes, OAuthScope, OAuthScopes, OAuthServer, TokenScopes }
import lila.ui.{ Page, Snippet }

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with lila.web.RequestGetter
    with lila.web.ResponseBuilder(using env.executor)
    with http.ResponseBuilder(using env.executor)
    with lila.web.ResponseHeaders
    with lila.web.ResponseWriter
    with lila.web.CtrlExtensions
    with http.CtrlFilters(using env.executor)
    with http.CtrlPage(using env.executor)
    with http.RequestContext(using env.executor)
    with lila.web.CtrlErrors:

  def controllerComponents = env.controllerComponents
  given Executor = env.executor
  given Scheduler = env.scheduler
  given FormBinding = parse.formBinding(parse.DefaultMaxTextLength)
  given lila.core.i18n.Translator = env.translator
  given reqBody(using r: BodyContext[?]): Request[?] = r.body

  given (using codec: Codec, pc: PageContext): Writeable[Page] =
    Writeable(page => codec.encode(views.base.page(page).html))

  given Conversion[Page, Fu[Page]] = fuccess(_)
  given Conversion[Snippet, Fu[Snippet]] = fuccess(_)

  given netDomain: lila.core.config.NetDomain = env.net.domain

  inline def ctx(using it: Context) = it // `ctx` is shorter and nicer than `summon[Context]`
  inline def req(using it: RequestHeader) = it // `req` is shorter and nicer than `summon[RequestHeader]`

  val limit = lila.web.Limiters(using env.executor, env.net.rateLimit)

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
    AuthOrScopedBodyWithParser(parse.anyContent)(selectors*)(f)

  def AuthOrScopedBodyWithParser[A](parser: BodyParser[A])(
      selectors: OAuthScope.Selector*
  )(f: BodyContext[A] ?=> Me ?=> Fu[Result]): EssentialAction =
    action(parser): req ?=>
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
  )(using C, Me): Fu[Result] =
    if isGranted(perm)
    then f.map(preventModCache(perm))
    else authorizationFailed

  private def preventModCache(perm: Permission.Selector)(res: Result) =
    if Permission.modPermissions(perm(Permission))
    then res.hasPersonalData
    else res

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

  private def handleScopedCommon(selectors: Seq[OAuthScope.Selector])(using
      req: RequestHeader
  )(f: OAuthScope.Scoped => Fu[Result]) =
    val accepted = OAuthScope.select(selectors).into(EndpointScopes)
    allow:
      for
        scoped <- env.security.api.oauthScoped(req, accepted)
        res <- f(scoped)
      yield OAuthServer.responseHeaders(accepted, scoped.scopes)(res)
    .rescue(handleScopedFail(accepted, _))

  def handleScopedFail(accepted: EndpointScopes, e: OAuthServer.AuthError) = e match
    case e @ lila.oauth.OAuthServer.MissingScope(_, available) =>
      OAuthServer.responseHeaders(accepted, available):
        forbiddenJson(e.message)
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
  def SecuredScopedBody(perm: Permission.Selector)(scopes: OAuthScope.Selector*)(
      f: BodyContext[?] ?=> Me ?=> Fu[Result]
  ) =
    ScopedBody(scopes*) { _ ?=> _ ?=>
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

  /* everyone on dev/stage, beta perm or https://lichess.org/team/lichess-beta-testers on prod */
  def Beta[A](f: Context ?=> Me ?=> Fu[Result]): EssentialAction =
    Auth { ctx ?=> _ ?=>
      if env.mode.notProd || isGrantedOpt(_.Beta)
      then f
      else ctx.myId.soUse(env.team.isBetaTester).flatMap(if _ then f else authorizationFailed)
    }

  def FormFuResult[A, B: Writeable](
      form: Form[A]
  )(err: Form[A] => Fu[B])(op: A => Fu[Result])(using Request[?]): Fu[Result] =
    bindForm(form)(
      form => err(form).dmap { BadRequest(_) },
      op
    )

  def HeadLastModifiedAt(updatedAt: Instant)(f: => Fu[Result])(using RequestHeader): Fu[Result] =
    if req.method == "HEAD" then NoContent.withDateHeaders(lastModified(updatedAt))
    else f

  def pageHit(using req: RequestHeader): Unit =
    if HTTPRequest.isHuman(req) then lila.mon.http.path(req.path).increment()

  def LangPage(call: Call)(f: Context ?=> Fu[Result])(language: Language): EssentialAction =
    LangPage(call.url)(f)(language)
  def LangPage(path: String)(f: Context ?=> Fu[Result])(language: Language): EssentialAction = Open:
    if ctx.isAuth
    then redirectWithQueryString(path)
    else
      import LangPicker.ByHref
      LangPicker.byHref(language, ctx.req) match
        case ByHref.NotFound => notFound(using ctx)
        case ByHref.Redir(code) =>
          redirectWithQueryString(s"/$code${~path.some.filter("/" !=)}")
        case ByHref.Refused(_) => redirectWithQueryString(path)
        case ByHref.Found(lang) =>
          f(using ctx.withLang(lang))

  def WithMyPerf[A](pt: lila.rating.PerfType)(f: Perf ?=> Fu[A])(using me: Option[Me]): Fu[A] = me
    .traverse(env.user.perfsRepo.perfOf(_, pt))
    .flatMap: perf =>
      f(using perf | lila.rating.Perf.default)
  def WithMyPerfs[A](f: Option[UserWithPerfs] ?=> Fu[A])(using me: Option[Me]): Fu[A] = me
    .traverse(me => env.user.api.withPerfs(me.value))
    .flatMap:
      f(using _)

  def meOrFetch[U: UserIdOf](id: U)(using ctx: Context): Fu[Option[lila.user.User]] =
    if id.is(UserId("me")) then fuccess(ctx.user)
    else ctx.user.filter(_.is(id)).fold(env.user.repo.byId(id))(u => fuccess(u.some))

  def meOrFetch[U: UserIdOf](id: Option[U])(using ctx: Context): Fu[Option[lila.user.User]] =
    id.fold(fuccess(ctx.user))(meOrFetch)

  given (using req: RequestHeader): lila.chat.AllMessages = lila.chat.AllMessages(HTTPRequest.isLitools(req))

  def anyCaptcha = env.game.captcha.any

  def bindForm[T, R](form: Form[T])(error: Form[T] => R, success: T => R)(using Request[?], FormBinding): R =
    val bound =
      if getBool("patch")
      then bindPatchForm(form)
      else form.bindFromRequest()
    bound.fold(error, success)

  private def bindPatchForm[T](form: Form[T])(using req: Request[?], formBinding: FormBinding): Form[T] =
    form.bind:
      // combine pre-filled data with request data
      formBinding(req).foldLeft(form.data) { case (s, (key, values)) =>
        if key.endsWith("[]") then
          val k = key.dropRight(2)
          s ++ values.zipWithIndex.map { (v, i) => s"$k[$i]" -> v }
        else s + (key -> ~values.headOption)
      }
