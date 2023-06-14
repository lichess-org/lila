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

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with ContentTypes
    with RequestGetter
    with ResponseWriter
    with CtrlExtensions:

  export _root_.router.ReverseRouterConversions.given

  def controllerComponents = env.controllerComponents
  given Executor           = env.executor
  given Scheduler          = env.scheduler

  protected given Zero[Result]             = Zero(Results.NotFound)
  protected given Conversion[Frag, Result] = Ok(_)
  protected given FormBinding              = parse.formBinding(parse.DefaultMaxTextLength)

  protected val keyPages = KeyPages(env)
  export keyPages.{ notFound as renderNotFound }

  protected val rateLimitedMsg             = "Too many requests. Try again later."
  protected val rateLimited                = Results.TooManyRequests(rateLimitedMsg)
  protected val rateLimitedJson            = Results.TooManyRequests(jsonError(rateLimitedMsg))
  protected val rateLimitedFu              = rateLimited.toFuccess
  protected def rateLimitedFu(msg: String) = Results.TooManyRequests(jsonError(msg)).toFuccess

  protected given (using RequestHeader): Conversion[Funit, Fu[Result]] =
    _ => negotiate(fuccess(Ok("ok")), _ => fuccess(jsonOkResult))

  given lila.common.config.NetDomain = env.net.domain

  inline def ctx(using it: WebContext)    = it // `ctx` is shorter and nicer than `summon[Context]`
  inline def req(using it: RequestHeader) = it // `req` is shorter and nicer than `summon[RequestHeader]`

  given (using ctx: AnyContext): Lang                              = ctx.lang
  given (using ctx: AnyContext): RequestHeader                     = ctx.req
  given (using ctx: AnyContext): UserContext                       = ctx.userContext
  given (using req: RequestHeader): ui.EmbedConfig                 = ui.EmbedConfig(req)
  given reqBody(using it: BodyContext[?]): play.api.mvc.Request[?] = it.body

  def reqLang(using req: RequestHeader): Lang = I18nLangPicker(req)

  /* Anonymous requests */
  protected def Anon(f: RequestHeader ?=> Fu[Result]): Action[Unit] =
    Action.async(parse.empty)(f(using _))

  /* Anonymous requests, with a body */
  protected def AnonBody(f: Request[?] ?=> Fu[Result]): Action[AnyContent] =
    Action.async(parse.anyContent)(f(using _))

  /* Anonymous requests, with a body */
  protected def AnonBodyOf[A](parser: BodyParser[A])(f: Request[A] ?=> A => Fu[Result]): Action[A] =
    Action.async(parser)(req => f(using req)(req.body))

  /* Anonymous and authenticated requests */
  protected def Open(f: WebContext ?=> Fu[Result]): Action[Unit] =
    OpenOf(parse.empty)(f)

  protected def OpenOf[A](parser: BodyParser[A])(f: WebContext ?=> Fu[Result]): Action[A] =
    Action.async(parser)(handleOpen(f, _))

  /* Anonymous and authenticated requests, with a body */
  protected def OpenBody(f: WebBodyContext[?] ?=> Fu[Result]): Action[AnyContent] =
    OpenBodyOf(parse.anyContent)(f)

  /* Anonymous and authenticated requests, with a body */
  protected def OpenBodyOf[A](parser: BodyParser[A])(f: WebBodyContext[A] ?=> Fu[Result]): Action[A] =
    Action.async(parser): req =>
      CSRF(req):
        reqToCtx(req).flatMap:
          f(using _)

  /* Anonymous, authenticated, and oauth requests */
  protected def OpenOrScoped(selectors: OAuthScope.Selector*)(
      open: WebContext ?=> Fu[Result],
      scoped: OAuthContext ?=> User => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(scoped)(req)
      else handleOpen(open, req)

  /* Anonymous, authenticated, and oauth requests */
  protected def OpenOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Option[User] => Fu[Result]
  ): Action[Unit] =
    OpenOrScoped(selectors*)(
      open = ctx ?=> f(ctx.me),
      scoped = ctx ?=> user => f(user.some)
    )

  private def handleOpen(f: WebContext ?=> Fu[Result], req: RequestHeader): Fu[Result] =
    CSRF(req):
      reqToCtx(req).flatMap:
        f(using _)

  // protected def OpenOrScopedBody(selectors: OAuthScope.Selector*)(
  //     open: BodyContext[_] => Fu[Result],
  //     scoped: Request[_] => User => Fu[Result]
  // ): Action[AnyContent] = OpenOrScopedBody(parse.anyContent)(selectors)(auth, scoped)

  /* Anonymous, authenticated, and oauth requests with a body */
  protected def OpenOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      f: BodyContext[A] ?=> Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(ctx ?=> _ => f(using ctx))(req)
      else OpenBodyOf(parser)(f)(req)

  /* Anonymous and oauth requests */
  protected def AnonOrScoped(selectors: OAuthScope.Selector*)(
      f: AnyContext ?=> Option[User] => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(ctx ?=> user => f(using ctx)(user.some))(req)
      else f(using anyContext(req))(none)

  /* Anonymous and oauth requests with a body */
  protected def AnonOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      f: BodyContext[A] ?=> Option[User] => Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(ctx ?=> user => f(using ctx)(user.some))(req)
      else f(using bodyContext(req))(none)

  /* Authenticated and oauth requests */
  protected def AuthOrScoped(selectors: OAuthScope.Selector*)(
      auth: WebContext ?=> User => Fu[Result],
      scoped: OAuthContext ?=> User => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then handleScoped(selectors)(scoped)(req)
      else handleAuth(auth, req)

  /* Authenticated and oauth requests with a body */
  protected def AuthOrScopedBody(selectors: OAuthScope.Selector*)(
      auth: WebBodyContext[?] ?=> User => Fu[Result],
      scoped: OAuthBodyContext[?] ?=> User => Fu[Result]
  ): Action[AnyContent] = AuthOrScopedBody(parse.anyContent)(selectors)(auth, scoped)

  /* Authenticated and oauth requests with a body */
  protected def AuthOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      auth: WebBodyContext[A] ?=> User => Fu[Result],
      scoped: OAuthBodyContext[A] ?=> User => Fu[Result]
  ): Action[A] =
    Action.async(parser): req =>
      if HTTPRequest.isOAuth(req)
      then ScopedBody(parser)(selectors)(scoped)(req)
      else AuthBody(parser)(auth)(req)

  /* Authenticated requests */
  protected def Auth(f: WebContext ?=> User => Fu[Result]): Action[Unit] =
    Auth(parse.empty)(f)

  /* Authenticated requests */
  protected def Auth[A](parser: BodyParser[A])(f: WebContext ?=> User => Fu[Result]): Action[A] =
    Action.async(parser) { handleAuth(f, _) }

  private def handleAuth(f: WebContext ?=> User => Fu[Result], req: RequestHeader): Fu[Result] =
    CSRF(req):
      reqToCtx(req).flatMap: ctx =>
        ctx.me.fold(authenticationFailed(using ctx))(f(using ctx))

  /* Authenticated requests with a body */
  protected def AuthBody(f: WebBodyContext[?] ?=> User => Fu[Result]): Action[AnyContent] =
    AuthBody(parse.anyContent)(f)

  /* Authenticated requests with a body */
  protected def AuthBody[A](
      parser: BodyParser[A]
  )(f: WebBodyContext[A] ?=> User => Fu[Result]): Action[A] =
    Action.async(parser): req =>
      CSRF(req):
        reqToCtx(req).flatMap: ctx =>
          ctx.me.fold(authenticationFailed(using ctx))(f(using ctx))

  /* Authenticated requests requiring certain permissions */
  protected def Secure(perm: Permission.Selector)(
      f: WebContext ?=> Holder => Fu[Result]
  ): Action[AnyContent] =
    Secure(parse.anyContent)(perm(Permission))(f)

  /* Authenticated requests requiring certain permissions */
  protected def Secure[A](
      parser: BodyParser[A]
  )(perm: Permission)(f: WebContext ?=> Holder => Fu[Result]): Action[A] =
    Auth(parser): me =>
      if isGranted(perm)
      then f(Holder(me))
      else authorizationFailed

  /* Authenticated requests requiring certain permissions, with a body */
  protected def SecureBody[A](
      parser: BodyParser[A]
  )(perm: Permission)(f: WebBodyContext[A] ?=> Holder => Fu[Result]): Action[A] =
    AuthBody(parser) { ctx ?=> me =>
      if isGranted(perm)
      then f(Holder(me))
      else authorizationFailed
    }

  /* Authenticated requests requiring certain permissions, with a body */
  protected def SecureBody(
      perm: Permission.Selector
  )(f: WebBodyContext[?] ?=> Holder => Fu[Result]): Action[AnyContent] =
    SecureBody(parse.anyContent)(perm(Permission))(f)

  /* OAuth requests */
  protected def Scoped[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthContext ?=> User => Fu[Result]): Action[A] =
    Action.async(parser)(handleScoped(selectors)(f))

  /* OAuth requests */
  protected def Scoped(
      selectors: OAuthScope.Selector*
  )(f: OAuthContext ?=> User => Fu[Result]): Action[Unit] =
    Scoped(parse.empty)(selectors)(f)

  /* OAuth requests with a body */
  protected def ScopedBody[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: OAuthBodyContext[A] ?=> User => Fu[Result]): Action[A] =
    Action.async(parser)(handleScopedBody(selectors)(f))

  /* OAuth requests with a body */
  protected def ScopedBody(
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

  protected def handleScopedFail(scopes: OAuthScopes, e: OAuthServer.AuthError) = e match
    case e @ lila.oauth.OAuthServer.MissingScope(available) =>
      lila.mon.user.oauth.request(false).increment()
      OAuthServer
        .responseHeaders(scopes, available):
          Forbidden(jsonError(e.message))
        .toFuccess
    case e =>
      lila.mon.user.oauth.request(false).increment()
      OAuthServer.responseHeaders(scopes, OAuthScopes(Nil)) { Unauthorized(jsonError(e.message)) }.toFuccess

  private def anyContext(request: RequestHeader): AnyContext = new:
    val userContext = UserContext(request, none, none, getAndSaveLang(request, none))
  private def bodyContext[A](request: Request[A]): BodyContext[A] = new:
    val userContext = UserContext(request, none, none, getAndSaveLang(request, none))
    def body        = request

  /* Authenticated and OAuth requests requiring certain permissions */
  protected def SecureOrScoped(perm: Permission.Selector)(
      secure: WebContext ?=> Holder => Fu[Result],
      scoped: OAuthContext ?=> Holder => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty): req =>
      if HTTPRequest.isOAuth(req)
      then SecuredScoped(perm)(scoped)(req)
      else Secure(parse.empty)(perm(Permission))(secure)(req)

  /* Authenticated and OAuth requests requiring certain permissions */
  protected def SecuredScoped(perm: Permission.Selector)(
      f: OAuthContext ?=> Holder => Fu[Result]
  ) =
    Scoped() { ctx ?=> me =>
      IfGranted(perm, ctx.req, me)(f(Holder(me)))
    }

  /* Authenticated and OAuth requests requiring certain permissions, with a body */
  protected def SecureOrScopedBody(perm: Permission.Selector)(
      secure: WebBodyContext[?] ?=> Holder => Fu[Result],
      scoped: OAuthBodyContext[?] ?=> Holder => Fu[Result]
  ): Action[AnyContent] =
    Action.async(parse.anyContent): req =>
      if HTTPRequest.isOAuth(req)
      then SecuredScopedBody(perm)(scoped)(req)
      else SecureBody(parse.anyContent)(perm(Permission))(secure)(req)

  /* OAuth requests requiring certain permissions, with a body */
  protected def SecuredScopedBody(perm: Permission.Selector)(
      f: OAuthBodyContext[?] ?=> Holder => Fu[Result]
  ) =
    ScopedBody() { ctx ?=> user =>
      IfGranted(perm)(f(Holder(user)))
    }

  def IfGranted(perm: Permission.Selector)(f: => Fu[Result])(using ctx: AnyContext): Fu[Result] =
    if isGranted(perm) then f
    else
      ctx match
        case web: WebContext => authorizationFailed(using web)
        case _               => authorizationFailed(ctx.req)

  def IfGranted(perm: Permission.Selector, req: RequestHeader, me: User)(f: => Fu[Result]): Fu[Result] =
    if isGranted(perm, me) then f else authorizationFailed(req)

  protected def Firewall[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if env.security.firewall.accepts(ctx.req) then a
    else keyPages.blacklisted.toFuccess

  protected def NoTor(res: => Fu[Result])(using ctx: WebContext) =
    if env.security.tor.isExitNode(ctx.ip)
    then Unauthorized(views.html.auth.bits.tor()).toFuccess
    else res

  protected def NoEngine[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if ctx.me.exists(_.marks.engine)
    then Forbidden(views.html.site.message.noEngine).toFuccess
    else a

  protected def NoBooster[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if ctx.me.exists(_.marks.boost)
    then Forbidden(views.html.site.message.noBooster).toFuccess
    else a

  protected def NoLame[A <: Result](a: => Fu[A])(using WebContext): Fu[Result] =
    NoEngine(NoBooster(a))

  protected def NoBot[A <: Result](a: => Fu[A])(using ctx: AnyContext): Fu[Result] =
    if ctx.isBot then
      ctx
        .match
          case web: WebContext => Forbidden(views.html.site.message.noBot(using web))
          case _               => Forbidden(jsonError("no bots allowed"))
        .toFuccess
    else a

  protected def NoLameOrBot[A <: Result](a: => Fu[A])(using WebContext): Fu[Result] =
    NoLame(NoBot(a))

  protected def NoLameOrBot[A <: Result](me: User)(a: => Fu[A]): Fu[Result] =
    if (me.isBot) notForBotAccounts.toFuccess
    else if (me.lame) Forbidden.toFuccess
    else a

  protected def NoShadowban[A <: Result](a: => Fu[A])(using ctx: WebContext): Fu[Result] =
    if (ctx.me.exists(_.marks.troll)) notFound else a

  protected def NoPlayban(a: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    ctx.userId
      .so(env.playban.api.currentBan)
      .flatMap:
        _.fold(a): ban =>
          negotiate(
            html = keyPages.home(Results.Forbidden),
            api = _ => playbanJsonError(ban)
          )

  protected def NoPlayban(userId: Option[UserId])(a: => Fu[Result]): Fu[Result] = userId
    .so(env.playban.api.currentBan)
    .flatMap:
      _.fold(a)(playbanJsonError)

  private def playbanJsonError(ban: lila.playban.TempBan) = fuccess:
    Forbidden(
      jsonError(
        s"Banned from playing for ${ban.remainingMinutes} minutes. Reason: Too many aborts, unplayed games, or rage quits."
      ) + ("minutes" -> JsNumber(ban.remainingMinutes))
    ) as JSON

  protected def NoCurrentGame(a: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    ctx.me.so(env.preloader.currentGameMyTurn) flatMap {
      _.fold(a): current =>
        negotiate(
          html = keyPages.home(Results.Forbidden),
          api = _ => currentGameJsonError(current)
        )
    }
  protected def NoCurrentGame(me: Option[User])(a: => Fu[Result]): Fu[Result] = me
    .so(env.preloader.currentGameMyTurn)
    .flatMap:
      _.fold(a)(currentGameJsonError)

  private def currentGameJsonError(current: lila.app.mashup.Preload.CurrentGame) = fuccess:
    Forbidden(
      jsonError:
        s"You are already playing ${current.opponent}"
    ) as JSON

  protected def NoPlaybanOrCurrent(a: => Fu[Result])(using WebContext): Fu[Result] =
    NoPlayban(NoCurrentGame(a))
  protected def NoPlaybanOrCurrent(me: Option[User])(a: => Fu[Result]): Fu[Result] =
    NoPlayban(me.map(_.id))(NoCurrentGame(me)(a))

  protected def JsonOk(body: JsValue): Result             = Ok(body) as JSON
  protected def JsonOk[A: Writes](body: A): Result        = Ok(Json toJson body) as JSON
  protected def JsonOk[A: Writes](fua: Fu[A]): Fu[Result] = fua dmap { JsonOk(_) }
  protected def JsonStrOk(str: JsonStr): Result           = Ok(str) as JSON
  protected def JsonBadRequest(body: JsValue): Result     = BadRequest(body) as JSON

  protected val jsonOkBody   = Json.obj("ok" -> true)
  protected val jsonOkResult = JsonOk(jsonOkBody)

  protected def JsonOptionOk[A: Writes](fua: Fu[Option[A]]) =
    fua.flatMap:
      _.fold(notFoundJson())(a => fuccess(JsonOk(a)))

  protected def FormFuResult[A, B: Writeable](
      form: Form[A]
  )(err: Form[A] => Fu[B])(op: A => Fu[Result])(using Request[?]) =
    form
      .bindFromRequest()
      .fold(
        form => err(form) dmap { BadRequest(_) },
        data => op(data)
      )

  protected def FuRedirect(fua: Fu[Call]) = fua map { Redirect(_) }

  protected def OptionOk[A, B: Writeable](
      fua: Fu[Option[A]]
  )(op: A => B)(using WebContext): Fu[Result] =
    OptionFuOk(fua): a =>
      fuccess(op(a))

  protected def OptionFuOk[A, B: Writeable](
      fua: Fu[Option[A]]
  )(op: A => Fu[B])(using ctx: WebContext) =
    fua flatMap { _.fold(notFound(using ctx))(a => op(a) dmap { Ok(_) }) }

  protected def OptionFuRedirect[A](fua: Fu[Option[A]])(op: A => Fu[Call])(using WebContext) =
    fua.flatMap:
      _.fold(notFound): a =>
        op(a).map:
          Redirect(_)

  protected def OptionFuRedirectUrl[A](fua: Fu[Option[A]])(op: A => Fu[String])(using WebContext) =
    fua.flatMap:
      _.fold(notFound): a =>
        op(a).map:
          Redirect(_)

  protected def OptionResult[A](fua: Fu[Option[A]])(op: A => Result)(using WebContext) =
    OptionFuResult(fua): a =>
      fuccess(op(a))

  protected def OptionFuResult[A](fua: Fu[Option[A]])(op: A => Fu[Result])(using WebContext) =
    fua flatMap { _.fold(notFound)(op) }

  def notFound(using ctx: WebContext): Fu[Result] =
    negotiate(
      html =
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then fuccess(renderNotFound)
        else fuccess(Results.NotFound("Resource not found")),
      api = _ => notFoundJson("Resource not found")
    )

  def jsonError[A: Writes](err: A): JsObject = Json.obj("error" -> err)

  def notFoundJsonSync(msg: String = "Not found"): Result = NotFound(jsonError(msg)) as JSON

  def notFoundJson(msg: String = "Not found"): Fu[Result] = fuccess(notFoundJsonSync(msg))

  def notForBotAccounts = JsonBadRequest(jsonError("This API endpoint is not for Bot accounts."))

  def ridiculousBackwardCompatibleJsonError(err: JsObject): JsObject =
    err ++ Json.obj("error" -> err)

  protected def notFoundReq(req: RequestHeader): Fu[Result] =
    reqToCtx(req).flatMap(notFound(using _))

  protected def isGranted(permission: Permission.Selector, user: User): Boolean =
    Granter(permission(Permission))(user)

  protected def isGranted(permission: Permission.Selector)(using AnyContext): Boolean =
    isGranted(permission(Permission))

  protected def isGranted(permission: Permission)(using ctx: AnyContext): Boolean =
    ctx.me so Granter(permission)

  protected def authenticationFailed(using ctx: WebContext): Fu[Result] =
    negotiate(
      html = fuccess:
        Redirect(
          if HTTPRequest.isClosedLoginPath(ctx.req)
          then routes.Auth.login
          else routes.Auth.signup
        ) withCookies env.lilaCookie.session(env.security.api.AccessUri, ctx.req.uri)
      ,
      api = _ =>
        env.lilaCookie
          .ensure(ctx.req):
            Unauthorized(jsonError("Login required"))
          .toFuccess
    )

  private val forbiddenJsonResult = Forbidden(jsonError("Authorization failed"))

  protected def authorizationFailed(using ctx: WebContext): Fu[Result] =
    negotiate(
      html =
        if HTTPRequest.isSynchronousHttp(ctx.req)
        then Forbidden(views.html.site.message.authFailed).toFuccess
        else Results.Forbidden("Authorization failed").toFuccess,
      api = _ => fuccess(forbiddenJsonResult)
    )
  protected def authorizationFailed(req: RequestHeader): Fu[Result] =
    negotiate(
      html = fuccess(Results.Forbidden("Authorization failed")),
      api = _ => fuccess(forbiddenJsonResult)
    )(using req)

  protected def negotiate(html: => Fu[Result], api: ApiVersion => Fu[Result])(using
      req: RequestHeader
  ): Fu[Result] =
    lila.api.Mobile.Api
      .requestVersion(req)
      .fold(html): v =>
        api(v).dmap(_ as JSON)
      .dmap(_.withHeaders("Vary" -> "Accept"))

  protected def reqToCtx(req: RequestHeader): Fu[WebContext] =
    restoreUser(req).flatMap: (d, impersonatedBy) =>
      val lang = getAndSaveLang(req, d.map(_.user))
      val ctx  = UserContext(req, d.map(_.user), impersonatedBy, lang)
      pageDataBuilder(ctx, d.exists(_.hasFingerPrint)) dmap { WebContext(ctx, _) }

  protected def reqToCtx[A](req: Request[A]): Fu[WebBodyContext[A]] =
    restoreUser(req).flatMap: (d, impersonatedBy) =>
      val lang = getAndSaveLang(req, d.map(_.user))
      val ctx  = UserBodyContext(req, d.map(_.user), impersonatedBy, lang)
      pageDataBuilder(ctx, d.exists(_.hasFingerPrint)) dmap { WebBodyContext(ctx, _) }

  private def getAndSaveLang(req: RequestHeader, user: Option[User]): Lang =
    val lang = I18nLangPicker(req, user.flatMap(_.lang))
    user.filter(_.lang.fold(true)(_ != lang.code)) foreach { env.user.repo.setLang(_, lang) }
    lang

  private def pageDataBuilder(ctx: UserContext, hasFingerPrint: Boolean): Fu[PageData] =
    val isPage = HTTPRequest isSynchronousHttp ctx.req
    val nonce  = isPage option Nonce.random
    ctx.me.fold(fuccess(PageData.anon(ctx.req, nonce, blindMode(using ctx)))) { me =>
      env.pref.api.getPref(me, ctx.req) zip {
        if (isPage)
          env.user.lightUserApi preloadUser me
          val enabledId = me.enabled.yes option me.id
          enabledId.so(env.team.api.nbRequests) zip
            enabledId.so(env.challenge.api.countInFor.get) zip
            enabledId.so(env.notifyM.api.unreadCount) zip
            env.mod.inquiryApi.forMod(me)
        else
          fuccess:
            (((0, 0), lila.notify.Notification.UnreadCount(0)), none)
      } map { case (pref, (((teamNbRequests, nbChallenges), nbNotifications), inquiry)) =>
        PageData(
          teamNbRequests,
          nbChallenges,
          nbNotifications,
          pref,
          blindMode = blindMode(using ctx),
          hasFingerprint = hasFingerPrint,
          hasClas = isGranted(_.Teacher, me) || env.clas.studentCache.isStudent(me.id),
          inquiry = inquiry,
          nonce = nonce
        )
      }
    }

  private def blindMode(using ctx: UserContext) =
    ctx.req.cookies.get(env.api.config.accessibility.blindCookieName) so { c =>
      c.value.nonEmpty && c.value == env.api.config.accessibility.hash
    }

  // user, impersonatedBy
  type RestoredUser = (Option[FingerPrintedUser], Option[User])
  private def restoreUser(req: RequestHeader): Fu[RestoredUser] =
    env.security.api restoreUser req dmap {
      case Some(Left(AppealUser(user))) if HTTPRequest.isClosedLoginPath(req) =>
        FingerPrintedUser(user, true).some
      case Some(Right(d)) if !env.net.isProd =>
        d.copy(user =
          d.user
            .addRole(lila.security.Permission.Beta.dbKey)
            .addRole(lila.security.Permission.Prismic.dbKey)
        ).some
      case Some(Right(d)) => d.some
      case _              => none
    } flatMap {
      case None => fuccess(None -> None)
      case Some(d) =>
        env.mod.impersonate.impersonating(d.user) map {
          _.fold[RestoredUser](d.some -> None): impersonated =>
            FingerPrintedUser(impersonated, hasFingerPrint = true).some -> d.user.some
        }
    }

  import env.security.csrfRequestHandler.check as csrfCheck
  protected val csrfForbiddenResult = Forbidden("Cross origin request forbidden").toFuccess

  private def CSRF(req: RequestHeader)(f: => Fu[Result]): Fu[Result] =
    if csrfCheck(req) then f else csrfForbiddenResult

  protected def XhrOnly(res: => Fu[Result])(using ctx: WebContext) =
    if HTTPRequest.isXhr(ctx.req) then res else notFound

  protected def XhrOrRedirectHome(res: => Fu[Result])(using ctx: WebContext) =
    if HTTPRequest.isXhr(ctx.req) then res
    else Redirect(routes.Lobby.home).toFuccess

  protected def Reasonable(
      page: Int,
      max: config.Max = config.Max(40),
      errorPage: => Fu[Result] = BadRequest("resource too old").toFuccess
  )(result: => Fu[Result]): Fu[Result] =
    if page < max.value && page > 0 then result else errorPage

  protected def NotForKids(f: => Fu[Result])(using ctx: WebContext) =
    if ctx.kid then notFound else f

  protected def NoCrawlers(result: => Fu[Result])(using ctx: WebContext) =
    if HTTPRequest.isCrawler(ctx.req).yes then notFound else result

  protected def NotManaged(result: => Fu[Result])(using ctx: WebContext) =
    ctx.me.so(env.clas.api.student.isManaged) flatMap {
      if _ then notFound else result
    }

  private val jsonGlobalErrorRenamer =
    import play.api.libs.json.*
    __.json update (
      (__ \ "global").json copyFrom (__ \ "").json.pick
    ) andThen (__ \ "").json.prune

  protected def errorsAsJson(form: Form[?])(using lang: Lang): JsObject =
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

  protected def apiFormError(form: Form[?]): JsObject =
    Json.obj("error" -> errorsAsJson(form)(using lila.i18n.defaultLang))

  protected def jsonFormError(err: Form[?])(using Lang) =
    fuccess(BadRequest(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))))

  protected def jsonFormErrorDefaultLang(err: Form[?]) =
    jsonFormError(err)(using lila.i18n.defaultLang)

  // TODO remove? use ctx?
  protected def jsonFormErrorFor(err: Form[?], req: RequestHeader, user: Option[User]) =
    jsonFormError(err)(using I18nLangPicker(req, user.flatMap(_.lang)))

  protected def newJsonFormError(err: Form[?])(using Lang) =
    fuccess(BadRequest(errorsAsJson(err)))

  protected def pageHit(req: RequestHeader): Unit =
    if HTTPRequest.isHuman(req) then lila.mon.http.path(req.path).increment().unit

  protected def pageHit(using ctx: WebContext): Unit = pageHit(ctx.req)

  protected val noProxyBufferHeader = "X-Accel-Buffering" -> "no"
  protected val noProxyBuffer       = (res: Result) => res.withHeaders(noProxyBufferHeader)
  protected def asAttachment(name: String) = (res: Result) =>
    res.withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=$name")
  protected def asAttachmentStream(name: String) = (res: Result) => noProxyBuffer(asAttachment(name)(res))

  protected val ndJsonContentType = "application/x-ndjson"
  protected val csvContentType    = "text/csv"

  protected def LangPage(call: Call)(f: WebContext ?=> Fu[Result])(langCode: String): Action[Unit] =
    LangPage(call.url)(f)(langCode)
  protected def LangPage(path: String)(f: WebContext ?=> Fu[Result])(langCode: String): Action[Unit] = Open:
    if ctx.isAuth
    then redirectWithQueryString(path)(ctx.req).toFuccess
    else
      import I18nLangPicker.ByHref
      I18nLangPicker.byHref(langCode, ctx.req) match
        case ByHref.NotFound => notFound(using ctx)
        case ByHref.Redir(code) =>
          redirectWithQueryString(s"/$code${~path.some.filter("/" !=)}")(ctx.req).toFuccess
        case ByHref.Refused(_) => redirectWithQueryString(path)(ctx.req).toFuccess
        case ByHref.Found(lang) =>
          pageHit(ctx.req)
          f(using ctx.withLang(lang))

  protected def redirectWithQueryString(path: String)(req: RequestHeader) =
    Redirect:
      if req.target.uriString.contains("?")
      then s"$path?${req.target.queryString}"
      else path

  protected val movedMap: Map[String, String] = Map(
    "swag" -> "https://shop.spreadshirt.com/lichess-org",
    "yt"   -> "https://www.youtube.com/c/LichessDotOrg",
    "dmca" -> "https://docs.google.com/forms/d/e/1FAIpQLSdRVaJ6Wk2KHcrLcY0BxM7lTwYSQHDsY2DsGwbYoLUBo3ngfQ/viewform",
    "fishnet" -> "https://github.com/lichess-org/fishnet",
    "qa"      -> "/faq",
    "help"    -> "/contact",
    "support" -> "/contact",
    "donate"  -> "/patron"
  )
  protected def staticRedirect(key: String): Option[Fu[Result]] =
    movedMap get key map { MovedPermanently(_).toFuccess }
