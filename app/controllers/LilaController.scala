package controllers

import ornicar.scalalib.Zero
import play.api.data.Form
import play.api.http._
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json.{ Json, JsValue, JsObject, JsArray, Writes }
import play.api.mvc._, Results._
import play.api.mvc.WebSocket.FrameFormatter
import play.twirl.api.Html
import scalaz.Monoid

import lila.api.{ PageData, Context, HeaderContext, BodyContext }
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import lila.security.{ Permission, Granter, FingerprintedUser }
import lila.user.{ UserContext, User => UserModel }

private[controllers] trait LilaController
    extends Controller
    with ContentTypes
    with RequestGetter
    with ResponseWriter {

  import Results._

  protected implicit val LilaResultZero = Zero.instance[Result](Results.NotFound)

  protected implicit val LilaHtmlMonoid = lila.app.templating.Environment.LilaHtmlMonoid

  protected implicit final class LilaPimpedResult(result: Result) {
    def fuccess = scala.concurrent.Future successful result
  }

  protected implicit def LilaHtmlToResult(content: Html): Result = Ok(content)

  protected implicit def LilaFunitToResult(funit: Funit)(implicit ctx: Context): Fu[Result] =
    negotiate(
      html = fuccess(Ok("ok")),
      api = _ => fuccess(Ok(Json.obj("ok" -> true)) as JSON))

  implicit def lang(implicit req: RequestHeader) = Env.i18n.pool lang req

  protected def NoCache(res: Result): Result = res.withHeaders(
    CACHE_CONTROL -> "no-cache, no-store, must-revalidate", EXPIRES -> "0"
  )

  protected def Socket[A: FrameFormatter](f: Context => Fu[(Iteratee[A, _], Enumerator[A])]) =
    WebSocket.tryAccept[A] { req => reqToCtx(req) flatMap f map scala.util.Right.apply }

  protected def SocketEither[A: FrameFormatter](f: Context => Fu[Either[Result, (Iteratee[A, _], Enumerator[A])]]) =
    WebSocket.tryAccept[A] { req => reqToCtx(req) flatMap f }

  protected def SocketOption[A: FrameFormatter](f: Context => Fu[Option[(Iteratee[A, _], Enumerator[A])]]) =
    WebSocket.tryAccept[A] { req =>
      reqToCtx(req) flatMap f map {
        case None       => Left(NotFound(Json.obj("error" -> "socket resource not found")))
        case Some(pair) => Right(pair)
      }
    }

  protected def Open(f: Context => Fu[Result]): Action[Unit] =
    Open(BodyParsers.parse.empty)(f)

  protected def Open[A](p: BodyParser[A])(f: Context => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      reqToCtx(req) flatMap { ctx =>
        Env.i18n.requestHandler.forUser(req, ctx.me).fold(f(ctx))(fuccess)
      }
    }

  protected def OpenBody(f: BodyContext[_] => Fu[Result]): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  protected def OpenBody[A](p: BodyParser[A])(f: BodyContext[A] => Fu[Result]): Action[A] =
    Action.async(p)(req => reqToCtx(req) flatMap f)

  protected def OpenNoCtx(f: RequestHeader => Fu[Result]): Action[AnyContent] =
    Action.async(f)

  protected def Auth(f: Context => UserModel => Fu[Result]): Action[Unit] =
    Auth(BodyParsers.parse.empty)(f)

  protected def Auth[A](p: BodyParser[A])(f: Context => UserModel => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      reqToCtx(req) flatMap { implicit ctx =>
        ctx.me.fold(authenticationFailed) { me =>
          Env.i18n.requestHandler.forUser(req, ctx.me).fold(f(ctx)(me))(fuccess)
        }
      }
    }

  protected def AuthBody(f: BodyContext[_] => UserModel => Fu[Result]): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  protected def AuthBody[A](p: BodyParser[A])(f: BodyContext[A] => UserModel => Fu[Result]): Action[A] =
    Action.async(p) { req =>
      reqToCtx(req) flatMap { implicit ctx =>
        ctx.me.fold(authenticationFailed)(me => f(ctx)(me))
      }
    }

  protected def Secure(perm: Permission.type => Permission)(f: Context => UserModel => Fu[Result]): Action[AnyContent] =
    Secure(perm(Permission))(f)

  protected def Secure(perm: Permission)(f: Context => UserModel => Fu[Result]): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  protected def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context => UserModel => Fu[Result]): Action[A] =
    Auth(p) { implicit ctx =>
      me =>
        isGranted(perm).fold(f(ctx)(me), fuccess(authorizationFailed(ctx.req)))
    }

  protected def SecureBody[A](p: BodyParser[A])(perm: Permission)(f: BodyContext[A] => UserModel => Fu[Result]): Action[A] =
    AuthBody(p) { implicit ctx =>
      me =>
        isGranted(perm).fold(f(ctx)(me), fuccess(authorizationFailed(ctx.req)))
    }

  protected def SecureBody(perm: Permission.type => Permission)(f: BodyContext[_] => UserModel => Fu[Result]): Action[AnyContent] =
    SecureBody(BodyParsers.parse.anyContent)(perm(Permission))(f)

  protected def Firewall[A <: Result](a: => Fu[A])(implicit ctx: Context): Fu[Result] =
    Env.security.firewall.accepts(ctx.req) flatMap {
      _ fold (a, {
        fuccess { Redirect(routes.Lobby.home()) }
      })
    }

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
            Forbidden(Json.obj(
              "error" -> s"Banned from playing for ${ban.remainingMinutes} minutes. Reason: Too many aborts or unplayed games"
            )) as JSON
          }
        )
      }
    }

  protected def NoCurrentGame(a: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.me.??(mashup.Preload.currentGame(Env.user.lightUser)) flatMap {
      _.fold(a) { current =>
        negotiate(
          html = Lobby.renderHome(Results.Forbidden),
          api = _ => fuccess {
            Forbidden(Json.obj(
              "error" -> s"You are already playing ${current.opponent}"
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
      op)

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
    api = _ => fuccess(Results.NotFound(Json.obj("error" -> "Resource not found")))
  )

  def notFoundJson(msg: String = "Not found"): Fu[Result] = fuccess {
    NotFound(Json.obj("error" -> msg))
  }

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

  protected val unauthorizedApiResult = Unauthorized(Json.obj("error" -> "Login required"))

  protected def authorizationFailed(req: RequestHeader): Result = Forbidden("no permission")

  protected def ensureSessionId(req: RequestHeader)(res: Result): Result =
    req.session.data.contains(LilaCookie.sessionId).fold(
      res,
      res withCookies LilaCookie.makeSessionId(req))

  protected def negotiate(html: => Fu[Result], api: Int => Fu[Result])(implicit ctx: Context): Fu[Result] =
    (lila.api.Mobile.Api.requestVersion(ctx.req) match {
      case Some(1) => api(1) map (_ as JSON)
      case _       => html
    }) map (_.withHeaders("Vary" -> "Accept"))

  protected def reqToCtx(req: RequestHeader): Fu[HeaderContext] =
    restoreUser(req) flatMap { d =>
      val ctx = UserContext(req, d.map(_.user))
      pageDataBuilder(ctx, d.??(_.hasFingerprint)) map { Context(ctx, _) }
    }

  protected def reqToCtx[A](req: Request[A]): Fu[BodyContext[A]] =
    restoreUser(req) flatMap { d =>
      val ctx = UserContext(req, d.map(_.user))
      pageDataBuilder(ctx, d.??(_.hasFingerprint)) map { Context(ctx, _) }
    }

  private def pageDataBuilder(ctx: UserContext, hasFingerprint: Boolean): Fu[PageData] =
    ctx.me.fold(fuccess(PageData anon blindMode(ctx))) { me =>
      val isPage = HTTPRequest.isSynchronousHttp(ctx.req)
      (Env.pref.api getPref me) zip {
        isPage ?? {
          import lila.hub.actorApi.relation._
          import akka.pattern.ask
          import makeTimeout.short
          (Env.hub.actor.relation ? GetOnlineFriends(me.id) map {
            case OnlineFriends(users) => users
          } recover { case _ => Nil }) zip
            Env.team.api.nbRequests(me.id) zip
            Env.message.api.unreadIds(me.id) zip
            Env.challenge.api.countInFor(me.id)
        }
      } map {
        case (pref, (((friends, teamNbRequests), messageIds), nbChallenges)) =>
          PageData(friends, teamNbRequests, messageIds.size, nbChallenges, pref,
            blindMode = blindMode(ctx),
            hasFingerprint = hasFingerprint)
      }
    }

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

  protected def XhrOnly(res: => Fu[Result])(implicit ctx: Context) =
    if (HTTPRequest isXhr ctx.req) res
    else notFound

  protected def Reasonable(page: Int, max: Int = 40)(result: => Fu[Result]): Fu[Result] =
    (page < max).fold(result, BadRequest("resource too old").fuccess)

  protected def NotForKids(f: => Fu[Result])(implicit ctx: Context) =
    if (ctx.kid) notFound else f

  protected def errorsAsJson(form: play.api.data.Form[_])(implicit lang: play.api.i18n.Messages) =
    lila.common.Form errorsAsJson form
}
