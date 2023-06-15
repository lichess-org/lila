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
import lila.oauth.{ OAuthScope, OAuthScopes, OAuthServer }
import lila.security.{ AppealUser, FingerPrintedUser, Granter, Permission }
import lila.user.{ Holder, User, UserContext, UserBodyContext }
import play.api.libs.json.Reads

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with lila.app.http.RequestGetter
    with lila.app.http.ResponseBuilder(using env.executor)
    with lila.app.http.ResponseWriter
    with lila.app.http.CtrlExtensions
    with lila.app.http.CtrlConversions
    with lila.app.http.CtrlFilters
    with lila.app.http.RequestContext(using env.executor):

  def controllerComponents = env.controllerComponents
  given Executor           = env.executor
  given Scheduler          = env.scheduler

  given FormBinding = parse.formBinding(parse.DefaultMaxTextLength)

  given lila.common.config.NetDomain = env.net.domain

  inline def ctx(using it: WebContext)    = it // `ctx` is shorter and nicer than `summon[Context]`
  inline def req(using it: RequestHeader) = it // `req` is shorter and nicer than `summon[RequestHeader]`

  def reqLang(using req: RequestHeader): Lang = I18nLangPicker(req)

  /* Anonymous requests */
  def Anon(f: RequestHeader ?=> Fu[Result]): Action[Unit] =
    Action.async(parse.empty)(f(using _))

  /* Anonymous requests, with a body */
  def AnonBody(f: Request[?] ?=> Fu[Result]): Action[AnyContent] =
    Action.async(parse.anyContent)(f(using _))

  /* Anonymous requests, with a body */
  def AnonBodyOf[A](parser: BodyParser[A])(f: Request[A] ?=> A => Fu[Result]): Action[A] =
    Action.async(parser)(req => f(using req)(req.body))

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
      scoped: OAuthContext ?=> User => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(scoped)(req)
      else handleOpen(open, req)

  /* Anonymous, authenticated, and oauth requests */
  def OpenOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Option[User] => Fu[Result]
  ): Action[Unit] =
    OpenOrScoped(selectors*)(
      open = ctx ?=> f(ctx.me),
      scoped = ctx ?=> user => f(user.some)
    )

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
      f: AnyContext ?=> Option[User] => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(ctx ?=> user => f(using ctx)(user.some))(req)
      else f(using anonContext(req))(none)

  /* Anonymous and oauth requests with a body */
  def AnonOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      f: BodyContext[A] ?=> Option[User] => Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(ctx ?=> user => f(using ctx)(user.some))(req)
      else f(using anonBodyContext(req))(none)

  /* Authenticated and oauth requests */
  def AuthOrScoped(selectors: OAuthScope.Selector*)(
      auth: WebContext ?=> User => Fu[Result],
      scoped: OAuthContext ?=> User => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(scoped)(req)
      else handleAuth(auth, req)

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
    Action.async(parser)(handleScoped(selectors)(f))

  /* OAuth requests */
  def Scoped(
      selectors: OAuthScope.Selector*
  )(f: OAuthContext ?=> User => Fu[Result]): Action[Unit] =
    Scoped(parse.empty)(selectors)(f)

  /* OAuth requests with a body */
  def ScopedBody[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthBodyContext[A] ?=> User => Fu[Result]): Action[A] =
    Action.async(parser)(handleScopedBody(selectors)(f))

  /* OAuth requests with a body */
  def ScopedBody(
      selectors: OAuthScope.Selector*
  )(f: OAuthBodyContext[?] ?=> User => Fu[Result]): Action[AnyContent] =
    ScopedBody(parse.anyContent)(selectors)(f)

  private def handleScoped(
      selectors: Seq[OAuthScope.Selector]
  )(f: OAuthContext ?=> User => Fu[Result])(req: RequestHeader): Fu[Result] =
    handleScopedCommon(selectors, req): (scoped, lang) =>
      val ctx = UserContext(req, scoped.user.some, none, lang)
      f(using OAuthContext(ctx, scoped.scopes))(scoped.user)

  private def handleScopedBody[A](
      selectors: Seq[OAuthScope.Selector]
  )(f: OAuthBodyContext[A] ?=> User => Fu[Result])(req: Request[A]): Fu[Result] =
    handleScopedCommon(selectors, req): (scoped, lang) =>
      val ctx = UserBodyContext(req, scoped.user.some, none, lang)
      f(using OAuthBodyContext(ctx, scoped.scopes))(scoped.user)

  private def handleScopedCommon(selectors: Seq[OAuthScope.Selector], req: RequestHeader)(
      f: (OAuthScope.Scoped, Lang) => Fu[Result]
  ) =
    val scopes = OAuthScope select selectors
    env.security.api.oauthScoped(req, scopes).flatMap {
      case Left(e) => handleScopedFail(scopes, e)
      case Right(scoped) =>
        lila.mon.user.oauth.request(true).increment()
        val lang = getAndSaveLang(req, scoped.user.some)
        f(scoped, lang) map OAuthServer.responseHeaders(scopes, scoped.scopes)
    }

  def handleScopedFail(scopes: OAuthScopes, e: OAuthServer.AuthError) = e match
    case e @ lila.oauth.OAuthServer.MissingScope(available) =>
      lila.mon.user.oauth.request(false).increment()
      OAuthServer
        .responseHeaders(scopes, available):
          Forbidden(jsonError(e.message))
    case e =>
      lila.mon.user.oauth.request(false).increment()
      OAuthServer.responseHeaders(scopes, OAuthScopes(Nil)):
        Unauthorized(jsonError(e.message))

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
      IfGranted(perm, ctx.req, me)(f(Holder(me)))
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

  def OptionFuResult[A](fua: Fu[Option[A]])(op: A => Fu[Result])(using WebContext): Fu[Result] =
    fua flatMap { _.fold(notFound)(op) }

  private val jsonGlobalErrorRenamer: Reads[JsObject] =
    import play.api.libs.json.*
    __.json update (
      (__ \ "global").json copyFrom (__ \ "").json.pick
    ) andThen (__ \ "").json.prune

  def errorsAsJson(form: Form[?])(using lang: Lang): JsObject =
    val json = JsObject:
      form.errors
        .groupBy(_.key)
        .view
        .mapValues: errors =>
          JsArray:
            errors.map: e =>
              JsString(lila.i18n.Translator.txt.literal(I18nKey(e.message), e.args, lang))
        .toMap
    json validate jsonGlobalErrorRenamer getOrElse json

  def apiFormError(form: Form[?]): JsObject =
    Json.obj("error" -> errorsAsJson(form)(using lila.i18n.defaultLang))

  def jsonFormError(err: Form[?])(using Lang) = fuccess:
    BadRequest(ridiculousBackwardCompatibleJsonError(errorsAsJson(err)))

  def jsonFormErrorDefaultLang(err: Form[?]) =
    jsonFormError(err)(using lila.i18n.defaultLang)

  def newJsonFormError(err: Form[?])(using Lang) = fuccess:
    BadRequest(errorsAsJson(err))

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
