package controllers

import lila._
import user.{ User ⇒ UserModel }
import security.{ AuthConfigImpl, Permission }
import http.{ Context, BodyContext, HttpEnvironment }
import core.Global

import jp.t2v.lab.play20.auth._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.http._

import scala.io.Codec
import com.codahale.jerkson.Json
import scalaz.effects._

trait LilaController
    extends Controller
    with HttpEnvironment
    with ContentTypes
    with RequestGetter
    with AuthConfigImpl
    with Auth {

  lazy val env = Global.env
  lazy val cache = env.mongodb.cache

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

  def Auth(f: Context ⇒ User ⇒ Result): Action[AnyContent] =
    Auth(BodyParsers.parse.anyContent)(f)

  def Auth[A](p: BodyParser[A])(f: Context ⇒ User ⇒ Result): Action[A] =
    Action(p)(req ⇒ {
      val ctx = reqToCtx(req)
      ctx.me.fold(me ⇒ f(ctx)(me), authenticationFailed(ctx.req))
    })

  def AuthBody(f: BodyContext ⇒ User ⇒ Result): Action[AnyContent] =
    AuthBody(BodyParsers.parse.anyContent)(f)

  def AuthBody[A](p: BodyParser[A])(f: BodyContext ⇒ User ⇒ Result): Action[A] =
    Action(p)(req ⇒ {
      val ctx = reqToCtx(req)
      ctx.me.fold(me ⇒ f(ctx)(me), authenticationFailed(ctx.req))
    })

  def Secure(perm: Permission)(f: Context ⇒ User ⇒ Result): Action[AnyContent] =
    Secure(BodyParsers.parse.anyContent)(perm)(f)

  def Secure[A](p: BodyParser[A])(perm: Permission)(f: Context ⇒ User ⇒ Result): Action[A] =
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
    ioa.unsafePerformIO.fold(a ⇒ Redirect(op(a)), notFound(ctx))

  def IOptionResult[A](ioa: IO[Option[A]])(op: A ⇒ Result)(implicit ctx: Context) =
    ioa.unsafePerformIO.fold(a ⇒ op(a), notFound(ctx))

  def notFound(ctx: Context) = Lobby handleNotFound ctx

  // I like Unit requests.
  implicit def wUnit: Writeable[Unit] =
    Writeable[Unit](_ ⇒ Codec toUTF8 "ok")
  implicit def ctoUnit: ContentTypeOf[Unit] =
    ContentTypeOf[Unit](Some(ContentTypes.TEXT))

  implicit def wFloat: Writeable[Float] =
    Writeable[Float](f ⇒ Codec toUTF8 f.toString)
  implicit def ctoFloat: ContentTypeOf[Float] =
    ContentTypeOf[Float](Some(ContentTypes.TEXT))

  implicit def wLong: Writeable[Long] =
    Writeable[Long](a ⇒ Codec toUTF8 a.toString)
  implicit def ctoLong: ContentTypeOf[Long] =
    ContentTypeOf[Long](Some(ContentTypes.TEXT))

  implicit def wInt: Writeable[Int] =
    Writeable[Int](i ⇒ Codec toUTF8 i.toString)
  implicit def ctoInt: ContentTypeOf[Int] =
    ContentTypeOf[Int](Some(ContentTypes.TEXT))

  implicit def wOptionString: Writeable[Option[String]] =
    Writeable[Option[String]](i ⇒ Codec toUTF8 i.getOrElse(""))
  implicit def ctoOptionString: ContentTypeOf[Option[String]] =
    ContentTypeOf[Option[String]](Some(ContentTypes.TEXT))

  implicit def richForm[A](form: Form[A]) = new {
    def toValid: Valid[A] = form.fold(
      form ⇒ failure(nel("Invalid form", form.errors.map(_.toString).toList)),
      data ⇒ success(data)
    )
  }

  protected def reqToCtx(req: Request[_]) = Context(req, restoreUser(req))

  protected def reqToCtx(req: RequestHeader) = Context(req, restoreUser(req))

  private def restoreUser[A](req: RequestHeader): Option[UserModel] = for {
    sessionId ← req.session.get("sessionId")
    userId ← cache.getAs[Id](sessionId + ":sessionId")(idManifest)
    user ← resolveUser(userId)
  } yield {
    cache.set(sessionId + ":sessionId", userId, sessionTimeoutInSeconds)
    cache.set(userId.toString + ":userId", sessionId, sessionTimeoutInSeconds)
    user
  }
}
