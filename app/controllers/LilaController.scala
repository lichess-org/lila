package controllers

import lila._
import user.{ User ⇒ UserModel }
import security.{ AuthImpl, Permission, Granter }
import http.{ Context, HeaderContext, BodyContext, HttpEnvironment }
import core.Global

import play.api.mvc._
import play.api.data.Form
import play.api.http._
import com.codahale.jerkson.Json
import scalaz.effects._

trait LilaController
    extends Controller
    with HttpEnvironment
    with ContentTypes
    with RequestGetter
    with ResponseWriter
    with AuthImpl {

  lazy val env = Global.env
  implicit val current = env.app

  override implicit def lang(implicit req: RequestHeader) =
    env.i18n.pool.lang(req)

  def toJson(map: Map[String, Any]) = Json generate map

  def Open(f: Context ⇒ Result): Action[AnyContent] =
    Open(BodyParsers.parse.anyContent)(f)

  def Open[A](p: BodyParser[A])(f: Context ⇒ Result): Action[A] =
    Action(p)(req ⇒ f(reqToCtx(req)))

  def OpenBody(f: BodyContext ⇒ Result): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  def OpenBody[A](p: BodyParser[A])(f: BodyContext ⇒ Result): Action[A] =
    Action(p)(req ⇒ f(reqToCtx(req)))

  def Auth(f: Context ⇒ UserModel ⇒ Result): Action[AnyContent] =
    Auth(BodyParsers.parse.anyContent)(f)

  def Auth[A](p: BodyParser[A])(f: Context ⇒ UserModel ⇒ Result): Action[A] =
    Action(p)(req ⇒ {
      val ctx = reqToCtx(req)
      ctx.me.fold(me ⇒ f(ctx)(me), authenticationFailed(ctx.req))
    })

  def AuthBody(f: BodyContext ⇒ UserModel ⇒ Result): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  def AuthBody[A](p: BodyParser[A])(f: BodyContext ⇒ UserModel ⇒ Result): Action[A] =
    Action(p)(req ⇒ {
      val ctx = reqToCtx(req)
      ctx.me.fold(me ⇒ f(ctx)(me), authenticationFailed(ctx.req))
    })

  def Secure(perm: Permission)(f: Context ⇒ UserModel ⇒ Result): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context ⇒ UserModel ⇒ Result): Action[A] =
    Auth(p) { ctx ⇒
      me ⇒
        ctx.isGranted(perm).fold(f(ctx)(me), authorizationFailed(ctx.req))
    }

  def JsonOk(map: Map[String, Any]) = Ok(toJson(map)) as JSON

  def JsonOk(list: List[String]) = Ok(Json generate list) as JSON

  def JsonIOk(map: IO[Map[String, Any]]) = JsonOk(map.unsafePerformIO)

  def ValidOk(valid: Valid[Unit]) = valid.fold(
    e ⇒ BadRequest(e.shows),
    _ ⇒ Ok("ok")
  )

  def ValidIOk(valid: IO[Valid[Unit]]) = valid.unsafePerformIO.fold(
    e ⇒ BadRequest(e.shows),
    _ ⇒ Ok("ok")
  )

  def FormResult[A](form: Form[A])(op: A ⇒ Result)(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ op(data)
    )

  def FormIOk[A](form: Form[A])(op: A ⇒ IO[Unit])(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ IOk(op(data))
    )

  def FormValidIOk[A](form: Form[A])(op: A ⇒ IO[Valid[Unit]])(implicit req: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ ValidIOk(op(data))
    )

  def IOk[A](op: IO[A])(implicit writer: Writeable[A], ctype: ContentTypeOf[A]) =
    Ok(op.unsafePerformIO)

  def IOResult[A](op: IO[Result]) =
    op.unsafePerformIO

  def IORedirect(op: IO[Call]) = Redirect(op.unsafePerformIO)

  def IOptionOk[A, B](ioa: IO[Option[A]])(op: A ⇒ B)(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    ctx: Context) =
    ioa.unsafePerformIO.fold(a ⇒ Ok(op(a)), notFound(ctx))

  def IOptionIOk[A, B](ioa: IO[Option[A]])(op: A ⇒ IO[B])(
    implicit writer: Writeable[B],
    ctype: ContentTypeOf[B],
    ctx: Context) =
    ioa flatMap { aOption ⇒
      aOption.fold(
        a ⇒ op(a) map { Ok(_) },
        io(notFound(ctx))): IO[Result]
    } unsafePerformIO

  def IOptionIOResult[A](ioa: IO[Option[A]])(op: A ⇒ IO[Result])(implicit ctx: Context) =
    ioa flatMap { _.fold(op, io(notFound(ctx))) } unsafePerformIO

  def IOptionRedirect[A](ioa: IO[Option[A]])(op: A ⇒ Call)(implicit ctx: Context) =
    ioa map {
      _.fold(a ⇒ Redirect(op(a)), io(notFound(ctx)))
    } unsafePerformIO

  def IOptionIORedirect[A](ioa: IO[Option[A]])(op: A ⇒ IO[Call])(implicit ctx: Context) =
    (ioa flatMap {
      _.fold(a ⇒ op(a) map { b ⇒ Redirect(b) }, io(notFound(ctx)))
    }: IO[Result]).unsafePerformIO

  def IOptionResult[A](ioa: IO[Option[A]])(op: A ⇒ Result)(implicit ctx: Context) =
    ioa.unsafePerformIO.fold(a ⇒ op(a), notFound(ctx))

  def notFound(ctx: Context) = Lobby handleNotFound ctx

  def todo = Open { implicit ctx ⇒
    NotImplemented(views.html.site.todo())
  }

  def isGranted(permission: Permission.type ⇒ Permission)(implicit ctx: Context): Boolean =
    Granter.option(permission(Permission))(ctx.me)

  protected def reqToCtx(req: Request[_]): BodyContext =
    Context(req, restoreUser(req) ~ setOnline)

  protected def reqToCtx(req: RequestHeader): HeaderContext =
    Context(req, restoreUser(req) ~ setOnline)

  private def setOnline(user: Option[UserModel]) {
    user foreach { u ⇒ env.user.usernameMemo.put(u.username).unsafePerformIO }
  }
}
