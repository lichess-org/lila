package controllers

import ornicar.scalalib.Zero
import play.api.data.Form
import play.api.data.FormBinding
import play.api.http._
import play.api.i18n.Lang
import play.api.libs.json.{ JsArray, JsObject, JsString, JsValue, Json, Writes }
import play.api.mvc._
import scala.annotation.nowarn
import scalatags.Text.Frag

import lila.api.{ BodyContext, Context, HeaderContext, PageData }
import lila.app._
import lila.common.{ ApiVersion, HTTPRequest, Nonce }
import lila.i18n.I18nLangPicker
import lila.notify.Notification.Notifies
import lila.oauth.{ OAuthScope, OAuthServer }
import lila.security.{ FingerPrintedUser, Granter, Permission }
import lila.user.{ UserContext, User => UserModel, Holder }

abstract private[controllers] class LilaController(val env: Env)
    extends BaseController
    with ContentTypes
    with RequestGetter
    with ResponseWriter {

  def controllerComponents      = env.controllerComponents
  implicit def executionContext = env.executionContext
  implicit def scheduler        = env.scheduler

  implicit protected val LilaResultZero = Zero.instance[Result](Results.NotFound)

  implicit final protected class LilaPimpedResult(result: Result) {
    def fuccess                           = scala.concurrent.Future successful result
    def flashSuccess(msg: String): Result = result.flashing("success" -> msg)
    def flashSuccess: Result              = flashSuccess("")
    def flashFailure(msg: String): Result = result.flashing("failure" -> msg)
    def flashFailure: Result              = flashFailure("")
  }

  implicit protected def LilaFragToResult(frag: Frag): Result = Ok(frag)

  implicit protected def makeApiVersion(v: Int) = ApiVersion(v)

  implicit protected lazy val formBinding: FormBinding = parse.formBinding(parse.DefaultMaxTextLength)

  protected val keyPages       = new KeyPages(env)
  protected val renderNotFound = keyPages.notFound _
  protected val rateLimited    = Results.TooManyRequests("Too many requests. Please retry in a moment.")
  protected val rateLimitedFu  = rateLimited.fuccess

  implicit protected def LilaFunitToResult(
      @nowarn("cat=unused") funit: Funit
  )(implicit req: RequestHeader): Fu[Result] =
    negotiate(
      html = fuccess(Ok("ok")),
      api = _ => fuccess(jsonOkResult)
    )

  implicit def ctxLang(implicit ctx: Context)         = ctx.lang
  implicit def ctxReq(implicit ctx: Context)          = ctx.req
  implicit def reqConfig(implicit req: RequestHeader) = ui.EmbedConfig(req)
  def reqLang(implicit req: RequestHeader)            = I18nLangPicker(req)

  protected def EnableSharedArrayBuffer(res: Result): Result =
    res.withHeaders(
      "Cross-Origin-Opener-Policy"   -> "same-origin",
      "Cross-Origin-Embedder-Policy" -> "require-corp"
    )

  protected def NoCache(res: Result): Result =
    res.withHeaders(
      CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
      EXPIRES       -> "0"
    )

  protected def Open(f: Context => Fu[Result]): Action[Unit] =
    Open(parse.empty)(f)

  protected def Open[A](parser: BodyParser[A])(f: Context => Fu[Result]): Action[A] =
    Action.async(parser)(handleOpen(f, _))

  protected def OpenBody(f: BodyContext[_] => Fu[Result]): Action[AnyContent] =
    OpenBody(parse.anyContent)(f)

  protected def OpenBody[A](parser: BodyParser[A])(f: BodyContext[A] => Fu[Result]): Action[A] =
    Action.async(parser) { req =>
      CSRF(req) {
        reqToCtx(req) flatMap f
      }
    }

  protected def OpenOrScoped(selectors: OAuthScope.Selector*)(
      open: Context => Fu[Result],
      scoped: RequestHeader => UserModel => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty) { req =>
      if (HTTPRequest isOAuth req) handleScoped(selectors)(scoped)(req)
      else handleOpen(open, req)
    }

  private def handleOpen(f: Context => Fu[Result], req: RequestHeader): Fu[Result] =
    CSRF(req) {
      reqToCtx(req) flatMap f
    }

  // protected def OpenOrScopedBody(selectors: OAuthScope.Selector*)(
  //     open: BodyContext[_] => Fu[Result],
  //     scoped: Request[_] => UserModel => Fu[Result]
  // ): Action[AnyContent] = OpenOrScopedBody(parse.anyContent)(selectors)(auth, scoped)

  protected def OpenOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      open: BodyContext[A] => Fu[Result],
      scoped: Request[A] => UserModel => Fu[Result]
  ): Action[A] =
    Action.async(parser) { req =>
      if (HTTPRequest isOAuth req) ScopedBody(parser)(selectors)(scoped)(req)
      else OpenBody(parser)(open)(req)
    }

  protected def AnonOrScoped(selectors: OAuthScope.Selector*)(
      anon: RequestHeader => Fu[Result],
      scoped: RequestHeader => UserModel => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty) { req =>
      if (HTTPRequest isOAuth req) handleScoped(selectors)(scoped)(req)
      else anon(req)
    }

  protected def AnonOrScopedBody[A](parser: BodyParser[A])(selectors: OAuthScope.Selector*)(
      anon: Request[A] => Fu[Result],
      scoped: Request[A] => UserModel => Fu[Result]
  ): Action[A] =
    Action.async(parser) { req =>
      if (HTTPRequest isOAuth req) ScopedBody(parser)(selectors)(scoped)(req)
      else anon(req)
    }

  protected def AuthOrScoped(selectors: OAuthScope.Selector*)(
      auth: Context => UserModel => Fu[Result],
      scoped: RequestHeader => UserModel => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty) { req =>
      if (HTTPRequest isOAuth req) handleScoped(selectors)(scoped)(req)
      else handleAuth(auth, req)
    }

  protected def AuthOrScopedBody(selectors: OAuthScope.Selector*)(
      auth: BodyContext[_] => UserModel => Fu[Result],
      scoped: Request[_] => UserModel => Fu[Result]
  ): Action[AnyContent] = AuthOrScopedBody(parse.anyContent)(selectors)(auth, scoped)

  protected def AuthOrScopedBody[A](parser: BodyParser[A])(selectors: Seq[OAuthScope.Selector])(
      auth: BodyContext[A] => UserModel => Fu[Result],
      scoped: Request[A] => UserModel => Fu[Result]
  ): Action[A] =
    Action.async(parser) { req =>
      if (HTTPRequest isOAuth req) ScopedBody(parser)(selectors)(scoped)(req)
      else AuthBody(parser)(auth)(req)
    }

  protected def Auth(f: Context => UserModel => Fu[Result]): Action[Unit] =
    Auth(parse.empty)(f)

  protected def Auth[A](parser: BodyParser[A])(f: Context => UserModel => Fu[Result]): Action[A] =
    Action.async(parser) { handleAuth(f, _) }

  private def handleAuth(f: Context => UserModel => Fu[Result], req: RequestHeader): Fu[Result] =
    CSRF(req) {
      reqToCtx(req) flatMap { ctx =>
        ctx.me.fold(authenticationFailed(ctx))(f(ctx))
      }
    }

  protected def AuthBody(f: BodyContext[_] => UserModel => Fu[Result]): Action[AnyContent] =
    AuthBody(parse.anyContent)(f)

  protected def AuthBody[A](parser: BodyParser[A])(f: BodyContext[A] => UserModel => Fu[Result]): Action[A] =
    Action.async(parser) { req =>
      CSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          ctx.me.fold(authenticationFailed(ctx))(f(ctx))
        }
      }
    }

  protected def Secure(perm: Permission.Selector)(f: Context => Holder => Fu[Result]): Action[AnyContent] =
    Secure(perm(Permission))(f)

  protected def Secure(perm: Permission)(f: Context => Holder => Fu[Result]): Action[AnyContent] =
    Secure(parse.anyContent)(perm)(f)

  protected def Secure[A](
      parser: BodyParser[A]
  )(perm: Permission)(f: Context => Holder => Fu[Result]): Action[A] =
    Auth(parser) { implicit ctx => me =>
      if (isGranted(perm)) f(ctx)(Holder(me)) else authorizationFailed
    }

  protected def SecureF(s: UserModel => Boolean)(f: Context => UserModel => Fu[Result]): Action[AnyContent] =
    Auth(parse.anyContent) { implicit ctx => me =>
      if (s(me)) f(ctx)(me) else authorizationFailed
    }

  protected def SecureBody[A](
      parser: BodyParser[A]
  )(perm: Permission)(f: BodyContext[A] => Holder => Fu[Result]): Action[A] =
    AuthBody(parser) { implicit ctx => me =>
      if (isGranted(perm)) f(ctx)(Holder(me)) else authorizationFailed
    }

  protected def SecureBody(
      perm: Permission.Selector
  )(f: BodyContext[_] => Holder => Fu[Result]): Action[AnyContent] =
    SecureBody(parse.anyContent)(perm(Permission))(f)

  protected def Scoped[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: RequestHeader => UserModel => Fu[Result]): Action[A] =
    Action.async(parser)(handleScoped(selectors)(f))

  protected def Scoped(
      selectors: OAuthScope.Selector*
  )(f: RequestHeader => UserModel => Fu[Result]): Action[Unit] =
    Scoped(parse.empty)(selectors)(f)

  protected def ScopedBody[A](
      parser: BodyParser[A]
  )(selectors: Seq[OAuthScope.Selector])(f: Request[A] => UserModel => Fu[Result]): Action[A] =
    Action.async(parser)(handleScoped(selectors)(f))

  protected def ScopedBody(
      selectors: OAuthScope.Selector*
  )(f: Request[_] => UserModel => Fu[Result]): Action[AnyContent] =
    ScopedBody(parse.anyContent)(selectors)(f)

  private def handleScoped[R <: RequestHeader](
      selectors: Seq[OAuthScope.Selector]
  )(f: R => UserModel => Fu[Result])(req: R): Fu[Result] = {
    val scopes = OAuthScope select selectors
    env.security.api.oauthScoped(req, scopes) flatMap {
      case Left(e) => handleScopedFail(scopes, e)
      case Right(scoped) =>
        lila.mon.user.oauth.request(true).increment()
        f(req)(scoped.user) map OAuthServer.responseHeaders(scopes, scoped.scopes)
    }
  }

  protected def handleScopedFail(scopes: Seq[OAuthScope], e: OAuthServer.AuthError) = e match {
    case e @ lila.oauth.OAuthServer.MissingScope(available) =>
      lila.mon.user.oauth.request(false).increment()
      OAuthServer
        .responseHeaders(scopes, available) {
          Unauthorized(jsonError(e.message))
        }
        .fuccess
    case e =>
      lila.mon.user.oauth.request(false).increment()
      OAuthServer.responseHeaders(scopes, Nil) { Unauthorized(jsonError(e.message)) }.fuccess
  }

  protected def SecureOrScoped(perm: Permission.Selector)(
      secure: Context => Holder => Fu[Result],
      scoped: RequestHeader => Holder => Fu[Result]
  ): Action[Unit] =
    Action.async(parse.empty) { req =>
      if (HTTPRequest isOAuth req) securedScopedAction(perm, req)(scoped)
      else Secure(parse.empty)(perm(Permission))(secure)(req)
    }
  protected def SecureOrScopedBody(perm: Permission.Selector)(
      secure: BodyContext[_] => Holder => Fu[Result],
      scoped: RequestHeader => Holder => Fu[Result]
  ): Action[AnyContent] =
    Action.async(parse.anyContent) { req =>
      if (HTTPRequest isOAuth req) securedScopedAction(perm, req.map(_ => ()))(scoped)
      else SecureBody(parse.anyContent)(perm(Permission))(secure)(req)
    }
  private def securedScopedAction(perm: Permission.Selector, req: Request[Unit])(
      f: RequestHeader => Holder => Fu[Result]
  ) =
    Scoped() { req => me =>
      IfGranted(perm, req, me)(f(req)(Holder(me)))
    }(req)

  def IfGranted(perm: Permission.Selector)(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    if (isGranted(perm)) f else authorizationFailed

  def IfGranted(perm: Permission.Selector, req: RequestHeader, me: UserModel)(f: => Fu[Result]): Fu[Result] =
    if (isGranted(perm, me)) f else authorizationFailed(req)

  protected def Firewall[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    if (env.security.firewall accepts ctx.req) a
    else keyPages.blacklisted.fuccess

  protected def NoTor(res: => Fu[Result])(implicit ctx: Context) =
    if (env.security.tor isExitNode HTTPRequest.ipAddress(ctx.req))
      Unauthorized(views.html.auth.bits.tor()).fuccess
    else res

  protected def NoEngine[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.exists(_.marks.engine)) Forbidden(views.html.site.message.noEngine).fuccess else a

  protected def NoBooster[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.exists(_.marks.boost)) Forbidden(views.html.site.message.noBooster).fuccess else a

  protected def NoLame[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    NoEngine(NoBooster(a))

  protected def NoBot[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.exists(_.isBot)) Forbidden(views.html.site.message.noBot).fuccess else a

  protected def NoLameOrBot[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    NoLame(NoBot(a))

  protected def NoShadowban[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    if (ctx.me.exists(_.marks.troll)) notFound else a

  protected def NoPlayban(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.userId.??(env.playban.api.currentBan) flatMap {
      _.fold(a) { ban =>
        negotiate(
          html = keyPages.home(Results.Forbidden),
          api = _ =>
            fuccess {
              Forbidden(
                jsonError(
                  s"Banned from playing for ${ban.remainingMinutes} minutes. Reason: Too many aborts, unplayed games, or rage quits."
                )
              ) as JSON
            }
        )
      }
    }

  protected def NoCurrentGame(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.me.??(env.preloader.currentGameMyTurn) flatMap {
      _.fold(a) { current =>
        negotiate(
          html = keyPages.home(Results.Forbidden),
          api = _ =>
            fuccess {
              Forbidden(
                jsonError(
                  s"You are already playing ${current.opponent}"
                )
              ) as JSON
            }
        )
      }
    }

  protected def NoPlaybanOrCurrent(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    NoPlayban(NoCurrentGame(a))

  protected def JsonOk(body: JsValue): Result             = Ok(body) as JSON
  protected def JsonOk[A: Writes](body: A): Result        = Ok(Json toJson body) as JSON
  protected def JsonOk[A: Writes](fua: Fu[A]): Fu[Result] = fua dmap { JsonOk(_) }

  protected val jsonOkBody   = Json.obj("ok" -> true)
  protected val jsonOkResult = JsonOk(jsonOkBody)

  protected def JsonOptionOk[A: Writes](fua: Fu[Option[A]]) =
    fua flatMap {
      _.fold(notFoundJson())(a => fuccess(JsonOk(a)))
    }

  protected def JsOk(fua: Fu[String], headers: (String, String)*) =
    fua map { a =>
      Ok(a) as JAVASCRIPT withHeaders (headers: _*)
    }

  protected def FormResult[A](form: Form[A])(op: A => Fu[Result])(implicit req: Request[_]): Fu[Result] =
    form
      .bindFromRequest()
      .fold(
        form => fuccess(BadRequest(form.errors mkString "\n")),
        op
      )

  protected def FormFuResult[A, B: Writeable: ContentTypeOf](
      form: Form[A]
  )(err: Form[A] => Fu[B])(op: A => Fu[Result])(implicit req: Request[_]) =
    form
      .bindFromRequest()
      .fold(
        form => err(form) dmap { BadRequest(_) },
        data => op(data)
      )

  protected def FuRedirect(fua: Fu[Call]) = fua map { Redirect(_) }

  protected def OptionOk[A, B: Writeable: ContentTypeOf](
      fua: Fu[Option[A]]
  )(op: A => B)(implicit ctx: Context): Fu[Result] =
    OptionFuOk(fua) { a =>
      fuccess(op(a))
    }

  protected def OptionFuOk[A, B: Writeable: ContentTypeOf](
      fua: Fu[Option[A]]
  )(op: A => Fu[B])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a => op(a) map { Ok(_) }) }

  protected def OptionFuRedirect[A](fua: Fu[Option[A]])(op: A => Fu[Call])(implicit ctx: Context) =
    fua flatMap {
      _.fold(notFound(ctx))(a =>
        op(a) map { b =>
          Redirect(b)
        }
      )
    }

  protected def OptionFuRedirectUrl[A](fua: Fu[Option[A]])(op: A => Fu[String])(implicit ctx: Context) =
    fua flatMap {
      _.fold(notFound(ctx))(a =>
        op(a) map { b =>
          Redirect(b)
        }
      )
    }

  protected def OptionResult[A](fua: Fu[Option[A]])(op: A => Result)(implicit ctx: Context) =
    OptionFuResult(fua) { a =>
      fuccess(op(a))
    }

  protected def OptionFuResult[A](fua: Fu[Option[A]])(op: A => Fu[Result])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a => op(a)) }

  def notFound(implicit ctx: Context): Fu[Result] =
    negotiate(
      html =
        if (HTTPRequest isSynchronousHttp ctx.req) fuccess(renderNotFound(ctx))
        else fuccess(Results.NotFound("Resource not found")),
      api = _ => notFoundJson("Resource not found")
    )

  def notFoundJson(msg: String = "Not found"): Fu[Result] =
    fuccess {
      NotFound(jsonError(msg))
    }

  def jsonError[A: Writes](err: A): JsObject = Json.obj("error" -> err)

  def notForBotAccounts =
    BadRequest(
      jsonError("This API endpoint is not for Bot accounts.")
    )

  def ridiculousBackwardCompatibleJsonError(err: JsObject): JsObject =
    err ++ Json.obj("error" -> err)

  protected def notFoundReq(req: RequestHeader): Fu[Result] =
    reqToCtx(req) flatMap (x => notFound(x))

  protected def isGranted(permission: Permission.Selector, user: UserModel): Boolean =
    Granter(permission(Permission))(user)

  protected def isGranted(permission: Permission.Selector)(implicit ctx: Context): Boolean =
    isGranted(permission(Permission))

  protected def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me ?? Granter(permission)

  protected def authenticationFailed(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = fuccess {
        Redirect(routes.Auth.signup) withCookies env.lilaCookie
          .session(env.security.api.AccessUri, ctx.req.uri)
      },
      api = _ =>
        env.lilaCookie
          .ensure(ctx.req) {
            Unauthorized(jsonError("Login required"))
          }
          .fuccess
    )

  private val forbiddenJsonResult = Forbidden(jsonError("Authorization failed"))

  protected def authorizationFailed(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = if (HTTPRequest isSynchronousHttp ctx.req) fuccess {
        Forbidden(views.html.site.message.authFailed)
      }
      else fuccess(Results.Forbidden("Authorization failed")),
      api = _ => fuccess(forbiddenJsonResult)
    )
  protected def authorizationFailed(req: RequestHeader): Fu[Result] =
    negotiate(
      html = fuccess(Results.Forbidden("Authorization failed")),
      api = _ => fuccess(forbiddenJsonResult)
    )(req)

  protected def negotiate(html: => Fu[Result], api: ApiVersion => Fu[Result])(implicit
      req: RequestHeader
  ): Fu[Result] =
    lila.api.Mobile.Api
      .requestVersion(req)
      .fold(html) { v =>
        api(v).dmap(_ as JSON)
      }
      .dmap(_.withHeaders("Vary" -> "Accept"))

  protected def reqToCtx(req: RequestHeader): Fu[HeaderContext] =
    restoreUser(req) flatMap { case (d, impersonatedBy) =>
      val lang = getAndSaveLang(req, d.map(_.user))
      val ctx  = UserContext(req, d.map(_.user), impersonatedBy, lang)
      pageDataBuilder(ctx, d.exists(_.hasFingerPrint)) dmap { Context(ctx, _) }
    }

  protected def reqToCtx[A](req: Request[A]): Fu[BodyContext[A]] =
    restoreUser(req) flatMap { case (d, impersonatedBy) =>
      val lang = getAndSaveLang(req, d.map(_.user))
      val ctx  = UserContext(req, d.map(_.user), impersonatedBy, lang)
      pageDataBuilder(ctx, d.exists(_.hasFingerPrint)) dmap { Context(ctx, _) }
    }

  private def getAndSaveLang(req: RequestHeader, user: Option[UserModel]): Lang = {
    val lang = I18nLangPicker(req, user.flatMap(_.lang))
    user.filter(_.lang.fold(true)(_ != lang.code)) foreach { env.user.repo.setLang(_, lang) }
    lang
  }

  private def pageDataBuilder(ctx: UserContext, hasFingerPrint: Boolean): Fu[PageData] = {
    val isPage = HTTPRequest isSynchronousHttp ctx.req
    val nonce  = isPage option Nonce.random
    ctx.me.fold(fuccess(PageData.anon(ctx.req, nonce, blindMode(ctx)))) { me =>
      env.pref.api.getPref(me, ctx.req) zip {
        if (isPage) {
          env.user.lightUserApi preloadUser me
          env.team.api.nbRequests(me.id) zip
            env.challenge.api.countInFor.get(me.id) zip
            env.notifyM.api.unreadCount(Notifies(me.id)).dmap(_.value) zip
            env.mod.inquiryApi.forMod(me)
        } else
          fuccess {
            (((0, 0), 0), none)
          }
      } map { case (pref, teamNbRequests ~ nbChallenges ~ nbNotifications ~ inquiry) =>
        PageData(
          teamNbRequests,
          nbChallenges,
          nbNotifications,
          pref,
          blindMode = blindMode(ctx),
          hasFingerprint = hasFingerPrint,
          hasClas = isGranted(_.Teacher, me) || env.clas.studentCache.isStudent(me.id),
          inquiry = inquiry,
          nonce = nonce
        )
      }
    }
  }

  private def blindMode(implicit ctx: UserContext) =
    ctx.req.cookies.get(env.api.config.accessibility.blindCookieName) ?? { c =>
      c.value.nonEmpty && c.value == env.api.config.accessibility.hash
    }

  // user, impersonatedBy
  type RestoredUser = (Option[FingerPrintedUser], Option[UserModel])
  private def restoreUser(req: RequestHeader): Fu[RestoredUser] =
    env.security.api restoreUser req dmap {
      case Some(d) if !env.net.isProd =>
        d.copy(user =
          d.user
            .addRole(lila.security.Permission.Beta.dbKey)
            .addRole(lila.security.Permission.Prismic.dbKey)
        ).some
      case d => d
    } flatMap {
      case None => fuccess(None -> None)
      case Some(d) =>
        env.mod.impersonate.impersonating(d.user) map {
          _.fold[RestoredUser](d.some -> None) { impersonated =>
            FingerPrintedUser(impersonated, hasFingerPrint = true).some -> d.user.some
          }
        }
    }

  protected val csrfCheck           = env.security.csrfRequestHandler.check _
  protected val csrfForbiddenResult = Forbidden("Cross origin request forbidden").fuccess

  private def CSRF(req: RequestHeader)(f: => Fu[Result]): Fu[Result] =
    if (csrfCheck(req)) f else csrfForbiddenResult

  protected def XhrOnly(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest isXhr ctx.req) res else notFound

  protected def XhrOrRedirectHome(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest isXhr ctx.req) res
    else Redirect(routes.Lobby.home).fuccess

  protected def Reasonable(
      page: Int,
      max: Int = 40,
      errorPage: => Fu[Result] = BadRequest("resource too old").fuccess
  )(result: => Fu[Result]): Fu[Result] =
    if (page < max) result else errorPage

  protected def NotForKids(f: => Fu[Result])(implicit ctx: Context) =
    if (ctx.kid) notFound else f

  protected def NotForBots(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest isCrawler ctx.req) notFound else res

  protected def OnlyHumans(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isCrawler ctx.req) fuccess(NotFound)
    else result

  protected def OnlyHumansAndFacebookOrTwitter(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isFacebookOrTwitterBot ctx.req) result
    else if (HTTPRequest isCrawler ctx.req) fuccess(NotFound)
    else result

  protected def NotManaged(result: => Fu[Result])(implicit ctx: Context) =
    ctx.me.??(env.clas.api.student.isManaged) flatMap {
      case true => notFound
      case _    => result
    }

  private val jsonGlobalErrorRenamer = {
    import play.api.libs.json._
    __.json update (
      (__ \ "global").json copyFrom (__ \ "").json.pick
    ) andThen (__ \ "").json.prune
  }

  protected def errorsAsJson(form: Form[_])(implicit lang: Lang): JsObject = {
    val json = JsObject(
      form.errors
        .groupBy(_.key)
        .view
        .mapValues { errors =>
          JsArray {
            errors.map { e =>
              JsString(lila.i18n.Translator.txt.literal(e.message, e.args, lang))
            }
          }
        }
        .toMap
    )
    json validate jsonGlobalErrorRenamer getOrElse json
  }

  protected def apiFormError(form: Form[_]): JsObject =
    Json.obj("error" -> errorsAsJson(form)(lila.i18n.defaultLang))

  protected def jsonFormError(err: Form[_])(implicit lang: Lang) =
    fuccess(BadRequest(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))))

  protected def jsonFormErrorDefaultLang(err: Form[_]) =
    jsonFormError(err)(lila.i18n.defaultLang)

  protected def jsonFormErrorFor(err: Form[_], req: RequestHeader, user: Option[UserModel]) =
    jsonFormError(err)(I18nLangPicker(req, user.flatMap(_.lang)))

  protected def newJsonFormError(err: Form[_])(implicit lang: Lang) =
    fuccess(BadRequest(errorsAsJson(err)))

  protected def pageHit(req: RequestHeader): Unit =
    if (HTTPRequest isHuman req) lila.mon.http.path(req.path).increment().unit

  protected def BadRequestWithReason(reason: String) = makeCustomResult(BAD_REQUEST, reason).pp

  protected def makeCustomResult(status: Int, reasonPhrase: String) =
    Result(
      header = new ResponseHeader(status, reasonPhrase = reasonPhrase.some).pp,
      body = play.api.http.HttpEntity.NoEntity
    )

  protected def pageHit(implicit ctx: lila.api.Context): Unit = pageHit(ctx.req)

  protected val noProxyBufferHeader = "X-Accel-Buffering" -> "no"
  protected val noProxyBuffer       = (res: Result) => res.withHeaders(noProxyBufferHeader)

  protected val pgnContentType    = "application/x-chess-pgn"
  protected val ndJsonContentType = "application/x-ndjson"
}
