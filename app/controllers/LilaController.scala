package controllers

import lila.app._
import lila.common.LilaCookie
import lila.user.{ Context, HeaderContext, BodyContext, User ⇒ UserModel }
import lila.security.{ Permission, Granter }

import play.api.mvc._, Results._
import play.api.data.Form
import play.api.http._
import play.api.libs.json.{ Json, Writes ⇒ WritesJson }

trait LilaController
    extends Controller
    with ContentTypes
    with RequestGetter
    with ResponseWriter {

  override implicit def lang(implicit req: RequestHeader) =
    Env.i18n.pool lang req

  protected def Open(f: Context ⇒ Fu[Result]): Action[AnyContent] =
    Open(BodyParsers.parse.anyContent)(f)

  protected def Open[A](p: BodyParser[A])(f: Context ⇒ Fu[Result]): Action[A] =
    Action(p)(req ⇒ Async(reqToCtx(req) flatMap f))

  protected def OpenBody(f: BodyContext ⇒ Fu[Result]): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  protected def OpenBody[A](p: BodyParser[A])(f: BodyContext ⇒ Fu[Result]): Action[A] =
    Action(p)(req ⇒ Async(reqToCtx(req) flatMap f))

  protected def Optional[A, B](foa: Fu[Option[A]])(op: A ⇒ B)(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    ctx: Context): Fu[Result] = foa flatMap {
    _.fold(notFound(ctx))(a ⇒ fuccess(Ok(op(a))))
  }

  // protected def Auth(f: Context ⇒ UserModel ⇒ Result): Action[AnyContent] =
  //   Auth(BodyParsers.parse.anyContent)(f)

  // protected def Auth[A](p: BodyParser[A])(f: Context ⇒ UserModel ⇒ Result): Action[A] =
  //   Action(p)(req ⇒ {
  //     val ctx = reqToCtx(req)
  //     ctx.me.fold(authenticationFailed(ctx.req))(me ⇒ f(ctx)(me))
  //   })

  // protected def AuthBody(f: BodyContext ⇒ UserModel ⇒ Result): Action[AnyContent] =
  //   AuthBody(BodyParsers.parse.anyContent)(f)

  // protected def AuthBody[A](p: BodyParser[A])(f: BodyContext ⇒ UserModel ⇒ Result): Action[A] =
  //   Action(p)(req ⇒ {
  //     val ctx = reqToCtx(req)
  //     ctx.me.fold(authenticationFailed(ctx.req))(me ⇒ f(ctx)(me))
  //   })

  // protected def Secure(perm: Permission)(f: Context ⇒ UserModel ⇒ Result): Action[AnyContent] =
  //   Secure(BodyParsers.parse.anyContent)(perm)(f)

  // protected def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context ⇒ UserModel ⇒ Result): Action[A] =
  //   Auth(p) { ctx ⇒
  //     me ⇒
  //       ctx.isGranted(perm).fold(f(ctx)(me), authorizationFailed(ctx.req))
  //   }

  // protected def Firewall[A <: Result](a: ⇒ A)(implicit ctx: Context): Result =
  //   env.security.firewall.accepts(ctx.req).fold(
  //     a, {
  //       env.security.firewall.logBlock(ctx.req)
  //       Redirect(routes.Lobby.home())
  //     }
  //   )

  // protected def NoEngine[A <: Result](a: ⇒ A)(implicit ctx: Context): Result =
  //   ctx.me.fold(false)(_.engine).fold(Forbidden(views.html.site.noEngine()), a)

  // protected def JsonOk[A: WritesJson](data: A) = Ok(toJson(data)) as JSON

  // protected def JsonIOk[A: WritesJson](data: IO[A]) = JsonOk(data.unsafePerformIO)

  // protected def JsIOk(js: IO[String], headers: (String, String)*) = 
  //   JsOk(js.unsafePerformIO, headers: _*)

  // protected def JsOk(js: String, headers: (String, String)*) = 
  //   Ok(js) as JAVASCRIPT withHeaders (headers: _*)

  // protected def ValidOk(valid: Valid[Unit]): Result = valid.fold(
  //   e ⇒ BadRequest(e.shows),
  //   _ ⇒ Ok("ok")
  // )

  // protected def ValidIOk(valid: IO[Valid[Unit]]): Result = ValidOk(valid.unsafePerformIO)

  protected def FormResult[A](form: Form[A])(op: A ⇒ Fu[Result])(implicit req: Request[_]): Fu[Result] =
    form.bindFromRequest.fold(
      form ⇒ fuccess(BadRequest(form.errors mkString "\n")),
      op)

  // protected def FormIOResult[A, B](form: Form[A])(err: Form[A] ⇒ B)(op: A ⇒ IO[Result])(
  //   implicit writer: Writeable[B],
  //   ctype: ContentTypeOf[B],
  //   req: Request[_]) =
  //   form.bindFromRequest.fold(
  //     form ⇒ BadRequest(err(form)),
  //     data ⇒ op(data).unsafePerformIO
  //   )

  // protected def IOk[A](op: IO[A])(implicit writer: Writeable[A], ctype: ContentTypeOf[A]) =
  //   Ok(op.unsafePerformIO)

  // protected def IOResult[A](op: IO[Result]) =
  //   op.unsafePerformIO

  // protected def IORedirect(op: IO[Call]) = Redirect(op.unsafePerformIO)

  // protected def IORedirectUrl(op: IO[String]) = Redirect(op.unsafePerformIO)

  // protected def OptionOk[A, B](oa: Option[A])(op: A ⇒ B)(
  //   implicit writer: Writeable[B],
  //   ctype: ContentTypeOf[B],
  //   ctx: Context) =
  //   oa.fold(notFound(ctx))(a ⇒ Ok(op(a)))

  // protected def OptionResult[A](oa: Option[A])(op: A ⇒ Result)(implicit ctx: Context) =
  //   oa.fold(notFound(ctx))(op)

  // protected def IOptionOk[A, B](ioa: IO[Option[A]])(op: A ⇒ B)(
  //   implicit writer: Writeable[B],
  //   ctype: ContentTypeOf[B],
  //   ctx: Context) =
  //   ioa.unsafePerformIO.fold(notFound(ctx))(a ⇒ Ok(op(a)))

  // protected def IOptionIOk[A, B](ioa: IO[Option[A]])(op: A ⇒ IO[B])(
  //   implicit writer: Writeable[B],
  //   ctype: ContentTypeOf[B],
  //   ctx: Context) =
  //   ioa flatMap { aOption ⇒
  //     aOption.fold(io(notFound(ctx))) { a ⇒ op(a) map { Ok(_) } } //: IO[Result]
  //   } unsafePerformIO

  // protected def IOptionIOResult[A](ioa: IO[Option[A]])(op: A ⇒ IO[Result])(implicit ctx: Context) =
  //   ioa flatMap { _.fold(io(notFound(ctx)))(op) } unsafePerformIO

  // protected def IOptionRedirect[A](ioa: IO[Option[A]])(op: A ⇒ Call)(implicit ctx: Context) =
  //   ioa map {
  //     _.fold(notFound(ctx))(a ⇒ Redirect(op(a)))
  //   } unsafePerformIO

  // protected def IOptionIORedirect[A](ioa: IO[Option[A]])(op: A ⇒ IO[Call])(implicit ctx: Context) =
  //   (ioa flatMap {
  //     _.fold(io(notFound(ctx)))(a ⇒ op(a) map { b ⇒ Redirect(b) })
  //   }: IO[Result]).unsafePerformIO

  // protected def IOptionIORedirectUrl[A](ioa: IO[Option[A]])(op: A ⇒ IO[String])(implicit ctx: Context) =
  //   (ioa flatMap {
  //     _.fold(io(notFound(ctx)))(a ⇒ op(a) map { b ⇒ Redirect(b) })
  //   }: IO[Result]).unsafePerformIO

  // protected def IOptionResult[A](ioa: IO[Option[A]])(op: A ⇒ Result)(implicit ctx: Context) =
  //   ioa.unsafePerformIO.fold(notFound(ctx))(a ⇒ op(a))

  protected def notFound(implicit ctx: Context): Fu[Result] =
    Lobby handleNotFound ctx

  // protected def todo = Open { implicit ctx ⇒
  //   NotImplemented(views.html.site.todo())
  // }

  // protected def isGranted(permission: Permission.type ⇒ Permission)(implicit ctx: Context): Boolean =
  //   Granter.option(permission(Permission))(ctx.me)

  protected def reqToCtx(req: Request[_]): Fu[BodyContext] =
    Env.security.api restoreUser req map { user ⇒
      setOnline(user)
      Context(req, user)
    }

  protected def reqToCtx(req: RequestHeader): Fu[HeaderContext] =
    Env.security.api restoreUser req map { user ⇒
      setOnline(user)
      Context(req, user)
    }

  private def setOnline(user: Option[UserModel]) {
    user foreach { u ⇒ Env.user.usernameMemo put u.username }
  }
}
