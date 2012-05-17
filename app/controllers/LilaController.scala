package controllers

import lila._
import user.{ User ⇒ UserModel }
import security.{ AuthConfigImpl }
import http.{ Context, BodyContext, HttpEnvironment }
import core.Global

import jp.t2v.lab.play20.auth._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.http._
import play.api.cache.Cache

import scala.io.Codec
import com.codahale.jerkson.Json
import scalaz.effects.IO

trait LilaController
    extends Controller
    with HttpEnvironment
    with ContentTypes
    with RequestGetter
    with AuthConfigImpl
    with Auth {

  lazy val env = Global.env

  implicit val current = env.app

  override implicit def lang(implicit req: RequestHeader) =
    env.i18n.pool.lang(req)

  def toJson(map: Map[String, Any]) = Json generate map

  def Open(f: Context ⇒ Result): Action[AnyContent] =
    Open(BodyParsers.parse.anyContent)(f)

  def Open[A](p: BodyParser[A])(f: Context ⇒ Result): Action[A] =
    Action(p)(req ⇒ f(Context(req, restoreUser(req))))

  def OpenBody(f: BodyContext ⇒ Result): Action[AnyContent] =
    OpenBody(BodyParsers.parse.anyContent)(f)

  def OpenBody[A](p: BodyParser[A])(f: BodyContext ⇒ Result): Action[A] =
    Action(p)(req ⇒ f(Context(req, restoreUser(req))))

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

  def IORedirect(op: IO[Call]) = Redirect(op.unsafePerformIO)

  def IOption[A, B](ioa: IO[Option[A]])(op: A ⇒ B)(implicit writer: Writeable[B], ctype: ContentTypeOf[B]) =
    ioa.unsafePerformIO.fold(a ⇒ Ok(op(a)), NotFound)

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

  implicit def richForm[A](form: Form[A]) = new {
    def toValid: Valid[A] = form.fold(
      form ⇒ failure(nel("Invalid form", form.errors.map(_.toString).toList)),
      data ⇒ success(data)
    )
  }

  private def restoreUser[A](request: Request[A]): Option[UserModel] = for {
    sessionId ← request.session.get("sessionId")
    userId ← Cache.getAs[Id](sessionId + ":sessionId")(current, idManifest)
    user ← resolveUser(userId)
  } yield {
    Cache.set(sessionId + ":sessionId", userId, sessionTimeoutInSeconds)
    Cache.set(userId.toString + ":userId", sessionId, sessionTimeoutInSeconds)
    user
  }
}
