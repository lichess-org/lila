package lila
package controllers

import play.api._
import mvc._
import data._
import http._

import scala.io.Codec
import com.codahale.jerkson.Json
import scalaz.effects.IO

trait LilaController extends Controller with ContentTypes with RequestGetter {

  lazy val env = Global.env

  def JsonOk(map: Map[String, Any]) = Ok(Json generate map) as JSON

  def JsonIOk(map: IO[Map[String, Any]]) = JsonOk(map.unsafePerformIO)

  def ValidOk(valid: Valid[Unit]) = valid.fold(
    e ⇒ BadRequest(e.shows),
    _ ⇒ Ok("ok")
  )

  def ValidIOk(valid: IO[Valid[Unit]]) = valid.unsafePerformIO.fold(
    e ⇒ BadRequest(e.shows),
    _ ⇒ Ok("ok")
  )

  def FormIOk[A](form: Form[A])(op: A ⇒ IO[Unit])(implicit request: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ IOk(op(data))
    )

  def FormValidIOk[A](form: Form[A])(op: A ⇒ IO[Valid[Unit]])(implicit request: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ ValidIOk(op(data))
    )

  def IOk(op: IO[Unit]) = Ok(op.unsafePerformIO)

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
}
