package controllers

import ornicar.scalalib.Zero
import play.api.data.Form
import play.api.http._
import play.api.libs.json.{ Json, JsObject, Writes }
import play.api.mvc._
import play.twirl.api.Html

import lila.api.{ PageData, Context, HeaderContext, BodyContext }
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest, ApiVersion }
import lila.notify.Notification.Notifies
import lila.security.{ Permission, Granter, FingerprintedUser }
import lila.user.{ UserContext, User => UserModel }

private[controllers] trait LilaController
    extends Controller
    with ContentTypes
    with RequestGetter
    with ResponseWriter
    with LilaSocket {

  protected val controllerLogger = lila.log("controller")

  protected implicit val LilaResultZero = Zero.instance[Result](Results.NotFound)

  protected implicit val LilaHtmlMonoid = lila.app.templating.Environment.LilaHtmlMonoid

  protected implicit final class LilaPimpedResult(result: Result) {
    def fuccess = scala.concurrent.Future successful result
  }

  protected implicit def LilaHtmlToResult(content: Html): Result = Ok(content)

  protected val jsonOkBody = Json.obj("ok" -> true)

  protected implicit def LilaFunitToResult(funit: Funit)(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = fuccess(Ok("ok")),
      api = _ => fuccess(Ok(jsonOkBody) as JSON)
    )

  implicit def lang(implicit ctx: Context) = ctx.lang

  protected def NoCache(res: Result): Result = res.withHeaders(
    CACHE_CONTROL -> "no-cache, no-store, must-revalidate", EXPIRES -> "0"
  )
  protected def NoIframe(res: Result): Result = res.withHeaders(
    "X-Frame-Options" -> "SAMEORIGIN"
  )

  protected def Open(f: Context => Fu[Result]): Action[Unit] =
    Open(BodyParsers.parse.empty)(f)

  protected def Open[A](p: BodyParser[A])(f: Context => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      CSRF(req) {
        reqToCtx(req) flatMap f
      }
    }

  protected def OpenBody(f: BodyContext[_] => Fu[Result]): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  protected def OpenBody[A](p: BodyParser[A])(f: BodyContext[A] => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      CSRF(req) {
        reqToCtx(req) flatMap f
      }
    }

  protected def Auth(f: Context => UserModel => Fu[Result]): Action[Unit] =
    Auth(BodyParsers.parse.empty)(f)

  protected def Auth[A](p: BodyParser[A])(f: Context => UserModel => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      CSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          ctx.me.fold(authenticationFailed(ctx))(f(ctx))
        }
      }
    }

  protected def AuthBody(f: BodyContext[_] => UserModel => Fu[Result]): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  protected def AuthBody[A](p: BodyParser[A])(f: BodyContext[A] => UserModel => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      CSRF(req) {
        reqToCtx(req) flatMap { ctx =>
          ctx.me.fold(authenticationFailed(ctx))(f(ctx))
        }
      }
    }

  protected def Secure(perm: Permission.type => Permission)(f: Context => UserModel => Fu[Result]): Action[AnyContent] =
    Secure(perm(Permission))(f)

  protected def Secure(perm: Permission)(f: Context => UserModel => Fu[Result]): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  protected def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context => UserModel => Fu[Result]): Action[A] =
    Auth(p) { implicit ctx => me =>
      isGranted(perm).fold(f(ctx)(me), authorizationFailed)
    }

  protected def SecureF(s: UserModel => Boolean)(f: Context => UserModel => Fu[Result]): Action[AnyContent] =
    Auth(BodyParsers.parse.anyContent) { implicit ctx => me =>
      s(me).fold(f(ctx)(me), authorizationFailed)
    }

  protected def SecureBody[A](p: BodyParser[A])(perm: Permission)(f: BodyContext[A] => UserModel => Fu[Result]): Action[A] =
    AuthBody(p) { implicit ctx => me =>
      isGranted(perm).fold(f(ctx)(me), authorizationFailed)
    }

  protected def SecureBody(perm: Permission.type => Permission)(f: BodyContext[_] => UserModel => Fu[Result]): Action[AnyContent] =
    SecureBody(BodyParsers.parse.anyContent)(perm(Permission))(f)

  protected def Firewall[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    Env.security.firewall.accepts(ctx.req) flatMap {
      _ fold (a, {
        fuccess { Redirect(routes.Lobby.home()) }
      })
    }

  protected def NoTor(res: => Fu[Result])(implicit ctx: Context) =
    if (Env.security.tor isExitNode HTTPRequest.lastRemoteAddress(ctx.req))
      Unauthorized(views.html.auth.tor()).fuccess
    else res

  protected def NoEngine[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    ctx.me.??(_.engine).fold(Forbidden(views.html.site.noEngine()).fuccess, a)

  protected def NoBooster[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    ctx.me.??(_.booster).fold(Forbidden(views.html.site.noBooster()).fuccess, a)

  protected def NoLame[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    NoEngine(NoBooster(a))

  protected def NoPlayban(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.userId.??(Env.playban.api.currentBan) flatMap {
      _.fold(a) { ban =>
        negotiate(
          html = Lobby.renderHome(Results.Forbidden),
          api = _ => fuccess {
            Forbidden(jsonError(
              s"Banned from playing for ${ban.remainingMinutes} minutes. Reason: Too many aborts or unplayed games"
            )) as JSON
          }
        )
      }
    }

  protected def NoCurrentGame(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.me.??(mashup.Preload.currentGame(Env.user.lightUserSync)) flatMap {
      _.fold(a) { current =>
        negotiate(
          html = Lobby.renderHome(Results.Forbidden),
          api = _ => fuccess {
            Forbidden(jsonError(
              s"You are already playing ${current.opponent}"
            )) as JSON
          }
        )
      }
    }

  protected def NoPlaybanOrCurrent(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    NoPlayban(NoCurrentGame(a))

  protected def JsonOk[A: Writes](fua: Fu[A]) = fua map { a =>
    Ok(Json toJson a) as JSON
  }

  protected def JsonOptionOk[A: Writes](fua: Fu[Option[A]])(implicit ctx: Context) = fua flatMap {
    _.fold(notFound(ctx))(a => fuccess(Ok(Json toJson a) as JSON))
  }

  protected def JsonOptionFuOk[A, B: Writes](fua: Fu[Option[A]])(op: A => Fu[B])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a => op(a) map { b => Ok(Json toJson b) as JSON }) }

  protected def JsOk(fua: Fu[String], headers: (String, String)*) =
    fua map { a => Ok(a) as JAVASCRIPT withHeaders (headers: _*) }

  protected def FormResult[A](form: Form[A])(op: A => Fu[Result])(implicit req: Request[_]): Fu[Result] =
    form.bindFromRequest.fold(
      form => fuccess(BadRequest(form.errors mkString "\n")),
      op
    )

  protected def FormFuResult[A, B: Writeable: ContentTypeOf](form: Form[A])(err: Form[A] => Fu[B])(op: A => Fu[Result])(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form => err(form) map { BadRequest(_) },
      data => op(data)
    )

  protected def FuRedirect(fua: Fu[Call]) = fua map { Redirect(_) }

  protected def OptionOk[A, B: Writeable: ContentTypeOf](fua: Fu[Option[A]])(op: A => B)(implicit ctx: Context): Fu[Result] =
    OptionFuOk(fua) { a => fuccess(op(a)) }

  protected def OptionFuOk[A, B: Writeable: ContentTypeOf](fua: Fu[Option[A]])(op: A => Fu[B])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a => op(a) map { Ok(_) }) }

  protected def OptionFuRedirect[A](fua: Fu[Option[A]])(op: A => Fu[Call])(implicit ctx: Context) =
    fua flatMap {
      _.fold(notFound(ctx))(a => op(a) map { b => Redirect(b) })
    }

  protected def OptionFuRedirectUrl[A](fua: Fu[Option[A]])(op: A => Fu[String])(implicit ctx: Context) =
    fua flatMap {
      _.fold(notFound(ctx))(a => op(a) map { b => Redirect(b) })
    }

  protected def OptionResult[A](fua: Fu[Option[A]])(op: A => Result)(implicit ctx: Context) =
    OptionFuResult(fua) { a => fuccess(op(a)) }

  protected def OptionFuResult[A](fua: Fu[Option[A]])(op: A => Fu[Result])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a => op(a)) }

  def notFound(implicit ctx: Context): Fu[Result] = negotiate(
    html =
    if (HTTPRequest isSynchronousHttp ctx.req) Main notFound ctx.req
    else fuccess(Results.NotFound("Resource not found")),
    api = _ => notFoundJson("Resource not found")
  )

  def notFoundJson(msg: String = "Not found"): Fu[Result] = fuccess {
    NotFound(jsonError(msg))
  }

  def jsonError[A: Writes](err: A): JsObject = Json.obj("error" -> err)

  protected def notFoundReq(req: RequestHeader): Fu[Result] =
    reqToCtx(req) flatMap (x => notFound(x))

  protected def isGranted(permission: Permission.type => Permission, user: UserModel): Boolean =
    Granter(permission(Permission))(user)

  protected def isGranted(permission: Permission.type => Permission)(implicit ctx: Context): Boolean =
    isGranted(permission(Permission))

  protected def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me ?? Granter(permission)

  protected def authenticationFailed(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = fuccess {
        implicit val req = ctx.req
        Redirect(routes.Auth.signup) withCookies LilaCookie.session(Env.security.api.AccessUri, req.uri)
      },
      api = _ => unauthorizedApiResult.fuccess
    )

  protected val unauthorizedApiResult = Unauthorized(jsonError("Login required"))

  protected def authorizationFailed(implicit ctx: Context): Fu[Result] = negotiate(
    html =
    if (HTTPRequest isSynchronousHttp ctx.req) Main authFailed ctx.req
    else fuccess(Results.Forbidden("Authorization failed")),
    api = _ => fuccess(Forbidden(jsonError("Authorization failed")))
  )

  protected def ensureSessionId(req: RequestHeader)(res: Result): Result =
    req.session.data.contains(LilaCookie.sessionId).fold(
      res,
      res withCookies LilaCookie.makeSessionId(req)
    )

  protected def negotiate(html: => Fu[Result], api: ApiVersion => Fu[Result])(implicit ctx: Context): Fu[Result] =
    (lila.api.Mobile.Api.requestVersion(ctx.req) match {
      case Some(v) => api(v) dmap (_ as JSON)
      case _ => html
    }).dmap(_.withHeaders("Vary" -> "Accept"))

  protected def reqToCtx(req: RequestHeader): Fu[HeaderContext] = restoreUser(req) flatMap { d =>
    val ctx = UserContext(req, d.map(_.user), lila.i18n.I18nLangPicker(req, d.map(_.user)))
    pageDataBuilder(ctx, d.exists(_.hasFingerprint)) dmap { Context(ctx, _) }
  }

  protected def reqToCtx[A](req: Request[A]): Fu[BodyContext[A]] =
    restoreUser(req) flatMap { d =>
      val ctx = UserContext(req, d.map(_.user), lila.i18n.I18nLangPicker(req, d.map(_.user)))
      pageDataBuilder(ctx, d.exists(_.hasFingerprint)) dmap { Context(ctx, _) }
    }

  private def pageDataBuilder(ctx: UserContext, hasFingerprint: Boolean): Fu[PageData] =
    ctx.me.fold(fuccess(PageData.anon(ctx.req, getAssetVersion, blindMode(ctx)))) { me =>
      import lila.relation.actorApi.OnlineFriends
      val isPage = HTTPRequest.isSynchronousHttp(ctx.req)
      Env.pref.api.getPref(me, ctx.req) zip {
        if (isPage) {
          Env.user.lightUserApi preloadUser me
          Env.relation.online.friendsOf(me.id) zip
            Env.team.api.nbRequests(me.id) zip
            Env.challenge.api.countInFor.get(me.id) zip
            Env.notifyModule.api.unreadCount(Notifies(me.id)).dmap(_.value) zip
            Env.mod.inquiryApi.forMod(me)
        } else fuccess {
          ((((OnlineFriends.empty, 0), 0), 0), none)
        }
      } map {
        case (pref, (onlineFriends ~ teamNbRequests ~ nbChallenges ~ nbNotifications ~ inquiry)) =>
          PageData(onlineFriends, teamNbRequests, nbChallenges, nbNotifications, pref,
            blindMode = blindMode(ctx),
            hasFingerprint = hasFingerprint,
            assetVersion = getAssetVersion,
            inquiry = inquiry)
      }
    }

  private def getAssetVersion = Env.api.assetVersion.get

  private def blindMode(implicit ctx: UserContext) =
    ctx.req.cookies.get(Env.api.Accessibility.blindCookieName) ?? { c =>
      c.value.nonEmpty && c.value == Env.api.Accessibility.hash
    }

  private def restoreUser(req: RequestHeader): Fu[Option[FingerprintedUser]] =
    Env.security.api restoreUser req addEffect {
      _ ifTrue (HTTPRequest isSynchronousHttp req) foreach { d =>
        Env.current.bus.publish(lila.user.User.Active(d.user), 'userActive)
      }
    }

  protected val csrfCheck = Env.security.csrfRequestHandler.check _
  protected val csrfForbiddenResult = Forbidden("Cross origin request forbidden").fuccess

  private def CSRF(req: RequestHeader)(f: => Fu[Result]): Fu[Result] =
    if (csrfCheck(req)) f else csrfForbiddenResult

  protected def XhrOnly(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest isXhr ctx.req) res else notFound

  protected def XhrOrRedirectHome(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest isXhr ctx.req) res
    else Redirect(routes.Lobby.home).fuccess

  protected def Reasonable(page: Int, max: Int = 40, errorPage: => Fu[Result] = BadRequest("resource too old").fuccess)(result: => Fu[Result]): Fu[Result] =
    if (page < max) result else errorPage

  protected def NotForKids(f: => Fu[Result])(implicit ctx: Context) =
    if (ctx.kid) notFound else f

  protected def NotForBots(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest.isBot(ctx.req)) notFound else res

  protected def OnlyHumans(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isBot ctx.req) fuccess(NotFound)
    else result

  protected def OnlyHumansAndFacebookOrTwitter(result: => Fu[Result])(implicit ctx: lila.api.Context) =
    if (HTTPRequest isFacebookOrTwitterBot ctx.req) result
    else if (HTTPRequest isBot ctx.req) fuccess(NotFound)
    else result

  private val jsonGlobalErrorRenamer = {
    import play.api.libs.json._
    __.json update (
      (__ \ "global").json copyFrom (__ \ "").json.pick
    ) andThen (__ \ "").json.prune
  }

  protected def errorsAsJson(form: play.api.data.Form[_])(implicit lang: play.api.i18n.Lang) = {
    val json = Json.toJson(
      form.errors.groupBy(_.key).mapValues { errors =>
        errors.map(e => lila.i18n.Translator.txt.literal(e.message, e.args, lang))
      }
    )
    json validate jsonGlobalErrorRenamer getOrElse json
  }

  protected val pgnContentType = "application/x-chess-pgn"
}
