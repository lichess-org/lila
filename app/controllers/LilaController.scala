package controllers

import ornicar.scalalib.Zero
import play.api.data.Form
import play.api.http._
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json.{ Json, JsValue, JsObject, JsArray, Writes }
import play.api.mvc._, Results._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.templates.Html
import scalaz.Monoid

import lila.api.{ PageData, Context, HeaderContext, BodyContext }
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import lila.security.{ Permission, Granter }
import lila.user.{ User => UserModel }

private[controllers] trait LilaController
    extends Controller
    with ContentTypes
    with RequestGetter
    with ResponseWriter {

  import Results._

  protected implicit val LilaSimpleResultZero = Zero.instance[SimpleResult](Results.NotFound)

  protected implicit val LilaHtmlMonoid = lila.app.templating.Environment.LilaHtmlMonoid

  protected implicit final class LilaPimpedSimpleResult(result: SimpleResult) {
    def fuccess = scala.concurrent.Future successful result
  }

  protected implicit def LilaHtmlToSimpleResult(content: Html): SimpleResult = Ok(content)

  protected implicit def LilaFunitToSimpleResult(funit: Funit): Fu[SimpleResult] = funit inject Ok("ok")

  override implicit def lang(implicit req: RequestHeader) =
    Env.i18n.pool lang req

  protected def Socket[A: FrameFormatter](f: Context => Fu[(Iteratee[A, _], Enumerator[A])]) =
    WebSocket.async[A] { req => reqToCtx(req) flatMap f }

  protected def Open(f: Context => Fu[SimpleResult]): Action[AnyContent] =
    Open(BodyParsers.parse.anyContent)(f)

  protected def Open[A](p: BodyParser[A])(f: Context => Fu[SimpleResult]): Action[A] =
    Action.async(p)(req => reqToCtx(req) flatMap f)

  protected def OpenBody(f: BodyContext => Fu[SimpleResult]): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  protected def OpenBody[A](p: BodyParser[A])(f: BodyContext => Fu[SimpleResult]): Action[A] =
    Action.async(p)(req => reqToCtx(req) flatMap f)

  protected def OpenNoCtx(f: RequestHeader => Fu[SimpleResult]): Action[AnyContent] =
    Action.async(f)

  protected def Auth(f: Context => UserModel => Fu[SimpleResult]): Action[AnyContent] =
    Auth(BodyParsers.parse.anyContent)(f)

  protected def Auth[A](p: BodyParser[A])(f: Context => UserModel => Fu[SimpleResult]): Action[A] =
    Action.async(p) { req =>
      reqToCtx(req) flatMap { ctx =>
        ctx.me.fold(authenticationFailed(ctx.req).fuccess)(me => f(ctx)(me))
      }
    }

  protected def AuthBody(f: BodyContext => UserModel => Fu[SimpleResult]): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  protected def AuthBody[A](p: BodyParser[A])(f: BodyContext => UserModel => Fu[SimpleResult]): Action[A] =
    Action.async(p) { req =>
      reqToCtx(req) flatMap { ctx =>
        ctx.me.fold(authenticationFailed(ctx.req).fuccess)(me => f(ctx)(me))
      }
    }

  protected def Secure(perm: Permission.type => Permission)(f: Context => UserModel => Fu[SimpleResult]): Action[AnyContent] =
    Secure(perm(Permission))(f)

  protected def Secure(perm: Permission)(f: Context => UserModel => Fu[SimpleResult]): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  protected def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context => UserModel => Fu[SimpleResult]): Action[A] =
    Auth(p) { implicit ctx =>
      me =>
        isGranted(perm).fold(f(ctx)(me), fuccess(authorizationFailed(ctx.req)))
    }

  protected def Firewall[A <: SimpleResult](a: => Fu[A])(implicit ctx: Context): Fu[SimpleResult] =
    Env.security.firewall.accepts(ctx.req) flatMap {
      _ fold (a, {
        Env.security.firewall.logBlock(ctx.req)
        fuccess { Redirect(routes.Lobby.home()) }
      })
    }

  protected def NoEngine[A <: SimpleResult](a: => Fu[A])(implicit ctx: Context): Fu[SimpleResult] =
    ctx.me.??(_.engine).fold(Forbidden(views.html.site.noEngine()).fuccess, a)

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

  protected def FormResult[A](form: Form[A])(op: A => Fu[SimpleResult])(implicit req: Request[_]): Fu[SimpleResult] =
    form.bindFromRequest.fold(
      form => fuccess(BadRequest(form.errors mkString "\n")),
      op)

  protected def FormFuResult[A, B: Writeable: ContentTypeOf](form: Form[A])(err: Form[A] => Fu[B])(op: A => Fu[SimpleResult])(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form => err(form) map { BadRequest(_) },
      data => op(data)
    )

  protected def FuRedirect(fua: Fu[Call]) = fua map { Redirect(_) }

  protected def OptionOk[A, B: Writeable: ContentTypeOf](fua: Fu[Option[A]])(op: A => B)(implicit ctx: Context): Fu[SimpleResult] =
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

  protected def OptionResult[A](fua: Fu[Option[A]])(op: A => SimpleResult)(implicit ctx: Context) =
    OptionFuResult(fua) { a => fuccess(op(a)) }

  protected def OptionFuResult[A](fua: Fu[Option[A]])(op: A => Fu[SimpleResult])(implicit ctx: Context) =
    fua flatMap { _.fold(notFound(ctx))(a => op(a)) }

  protected def notFound(implicit ctx: Context): Fu[SimpleResult] =
    if (HTTPRequest isSynchronousHttp ctx.req) Lobby renderHome Results.NotFound
    else Results.NotFound("resource not found").fuccess

  protected def notFoundReq(req: RequestHeader): Fu[SimpleResult] =
    reqToCtx(req) flatMap (x => notFound(x))

  protected def isGranted(permission: Permission.type => Permission)(implicit ctx: Context): Boolean =
    isGranted(permission(Permission))

  protected def isGranted(permission: Permission)(implicit ctx: Context): Boolean =
    ctx.me ?? Granter(permission)

  protected def authenticationFailed(implicit req: RequestHeader): SimpleResult =
    Redirect(routes.Auth.signup) withCookies LilaCookie.session(Env.security.api.AccessUri, req.uri)

  protected def authorizationFailed(req: RequestHeader): SimpleResult =
    Forbidden("no permission")

  protected def reqToCtx(req: RequestHeader): Fu[HeaderContext] =
    restoreUser(req) map { lila.user.UserContext(req, _) } flatMap { ctx =>
      pageDataBuilder(ctx) map { Context(ctx, _) }
    }

  protected def reqToCtx(req: Request[_]): Fu[BodyContext] =
    restoreUser(req) map { lila.user.UserContext(req, _) } flatMap { ctx =>
      pageDataBuilder(ctx) map { Context(ctx, _) }
    }

  private def pageDataBuilder(ctx: lila.user.UserContext): Fu[PageData] =
    ctx.me.fold(fuccess(PageData.default)) { me =>
      val isPage = HTTPRequest.isSynchronousHttp(ctx.req)
      (Env.pref.api getPref me) zip {
        isPage ?? {
          import lila.hub.actorApi.relation._
          import akka.pattern.ask
          import makeTimeout.short
          (Env.hub.actor.relation ? GetOnlineFriends(me.id) map {
            case OnlineFriends(users) => users
          } recover { case _ => Nil }) zip Env.team.api.nbRequests(me.id)
        }
      } map {
        case (pref, (friends, teamNbRequests)) => PageData(friends, teamNbRequests, pref)
      }
    }

  private def restoreUser(req: RequestHeader): Fu[Option[UserModel]] =
    Env.security.api restoreUser req addEffect {
      _ ifTrue (HTTPRequest isSynchronousHttp req) foreach { user =>
        val lang = Env.i18n.pool.lang(req).language
        Env.current.bus.publish(lila.user.User.Active(user, lang), 'userActive)
      }
    }

  protected def Reasonable(page: Int, max: Int = 40)(result: => Fu[SimpleResult]): Fu[SimpleResult] =
    (page < max).fold(result, BadRequest("resource too old").fuccess)
}
