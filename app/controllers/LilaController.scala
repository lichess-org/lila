package controllers

import lila._
import user.{ User ⇒ UserModel }
import security.{ AuthImpl, Permission, Granter }
import http.{ Context, HeaderContext, BodyContext }
import core.Global

import play.api.mvc._
import play.api.data.Form
import play.api.http._
import com.codahale.jerkson.Json
import scalaz.effects._

trait LilaController
    extends Controller
    with ContentTypes
    with RequestGetter
    with ResponseWriter
    with AuthImpl {

  protected lazy val env = Global.env
  protected implicit def current = env.app

  override implicit def lang(implicit req: RequestHeader) =
    env.i18n.pool.lang(req)

  protected def toJson(map: Map[String, Any]) = Json generate map

  protected def Open(f: Context ⇒ Result): Action[AnyContent] =
    Open(BodyParsers.parse.anyContent)(f)

  protected def Open[A](p: BodyParser[A])(f: Context ⇒ Result): Action[A] =
    Action(p)(req ⇒ f(reqToCtx(req)))

  protected def OpenBody(f: BodyContext ⇒ Result): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  protected def OpenBody[A](p: BodyParser[A])(f: BodyContext ⇒ Result): Action[A] =
    Action(p)(req ⇒ f(reqToCtx(req)))

  protected def Auth(f: Context ⇒ UserModel ⇒ Result): Action[AnyContent] =
    Auth(BodyParsers.parse.anyContent)(f)

  protected def Auth[A](p: BodyParser[A])(f: Context ⇒ UserModel ⇒ Result): Action[A] =
    Action(p)(req ⇒ {
      val ctx = reqToCtx(req)
      ctx.me.fold(me ⇒ f(ctx)(me), authenticationFailed(ctx.req))
    })

  protected def AuthBody(f: BodyContext ⇒ UserModel ⇒ Result): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  protected def AuthBody[A](p: BodyParser[A])(f: BodyContext ⇒ UserModel ⇒ Result): Action[A] =
    Action(p)(req ⇒ {
      val ctx = reqToCtx(req)
      ctx.me.fold(me ⇒ f(ctx)(me), authenticationFailed(ctx.req))
    })

  protected def Secure(perm: Permission)(f: Context ⇒ UserModel ⇒ Result): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  protected def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context ⇒ UserModel ⇒ Result): Action[A] =
    Auth(p) { ctx ⇒
      me ⇒
        ctx.isGranted(perm).fold(f(ctx)(me), authorizationFailed(ctx.req))
    }

  protected def Firewall[A <: Result](a: ⇒ A)(implicit ctx: Context): Result =
    env.security.firewall.accepts(ctx.req).fold(
      a, {
        env.security.firewall.logBlock(ctx.req)
        Redirect(routes.Lobby.home())
      }
    )

  protected def JsonOk(map: Map[String, Any]) = Ok(toJson(map)) as JSON

  protected def JsonOk(list: List[Any]) = Ok(Json generate list) as JSON

  protected def JsonIOk(map: IO[Map[String, Any]]) = JsonOk(map.unsafePerformIO)

  protected def ValidOk(valid: Valid[Unit]) = valid.fold(
    e ⇒ BadRequest(e.shows),
    _ ⇒ Ok("ok")
  )

  protected def ValidIOk(valid: IO[Valid[Unit]]) = valid.unsafePerformIO.fold(
    e ⇒ BadRequest(e.shows),
    _ ⇒ Ok("ok")
  )

  protected def FormResult[A](form: Form[A])(op: A ⇒ Result)(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ op(data)
    )

  protected def FormIOResult[A, B](form: Form[A])(err: Form[A] ⇒ B)(op: A ⇒ IO[Result])(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    req: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(err(form)),
      data ⇒ op(data).unsafePerformIO
    )

  protected def IOk[A](op: IO[A])(implicit writer: Writeable[A], ctype: ContentTypeOf[A]) =
    Ok(op.unsafePerformIO)

  protected def IOResult[A](op: IO[Result]) =
    op.unsafePerformIO

  protected def IORedirect(op: IO[Call]) = Redirect(op.unsafePerformIO)

  protected def IORedirectUrl(op: IO[String]) = Redirect(op.unsafePerformIO)

  protected def OptionOk[A, B](oa: Option[A])(op: A ⇒ B)(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    ctx: Context) =
    oa.fold(a ⇒ Ok(op(a)), notFound(ctx))

  protected def OptionResult[A](oa: Option[A])(op: A ⇒ Result)(implicit ctx: Context) =
    oa.fold(op, notFound(ctx))

  protected def IOptionOk[A, B](ioa: IO[Option[A]])(op: A ⇒ B)(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    ctx: Context) =
    ioa.unsafePerformIO.fold(a ⇒ Ok(op(a)), notFound(ctx))

  protected def IOptionIOk[A, B](ioa: IO[Option[A]])(op: A ⇒ IO[B])(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    ctx: Context) =
    ioa flatMap { aOption ⇒
      aOption.fold(
        a ⇒ op(a) map { Ok(_) },
        io(notFound(ctx))): IO[Result]
    } unsafePerformIO

  protected def IOptionIOResult[A](ioa: IO[Option[A]])(op: A ⇒ IO[Result])(implicit ctx: Context) =
    ioa flatMap { _.fold(op, io(notFound(ctx))) } unsafePerformIO

  protected def IOptionRedirect[A](ioa: IO[Option[A]])(op: A ⇒ Call)(implicit ctx: Context) =
    ioa map {
      _.fold(a ⇒ Redirect(op(a)), io(notFound(ctx)))
    } unsafePerformIO

  protected def IOptionIORedirect[A](ioa: IO[Option[A]])(op: A ⇒ IO[Call])(implicit ctx: Context) =
    (ioa flatMap {
      _.fold(a ⇒ op(a) map { b ⇒ Redirect(b) }, io(notFound(ctx)))
    }: IO[Result]).unsafePerformIO

  protected def IOptionResult[A](ioa: IO[Option[A]])(op: A ⇒ Result)(implicit ctx: Context) =
    ioa.unsafePerformIO.fold(a ⇒ op(a), notFound(ctx))

  protected def notFound(implicit ctx: Context) = Lobby handleNotFound ctx

  protected def todo = Open { implicit ctx ⇒
    NotImplemented(views.html.site.todo())
  }

  protected def isGranted(permission: Permission.type ⇒ Permission)(implicit ctx: Context): Boolean =
    Granter.option(permission(Permission))(ctx.me)

  protected def reqToCtx(req: Request[_]): BodyContext =
    Context(req, restoreUser(req) ~ setOnline)

  protected def reqToCtx(req: RequestHeader): HeaderContext =
    Context(req, restoreUser(req) ~ setOnline)

  private def setOnline(user: Option[UserModel]) {
    user foreach { u ⇒ env.user.usernameMemo.put(u.username).unsafePerformIO }
  }
}
