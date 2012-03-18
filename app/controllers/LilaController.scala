package controllers

import lila.http._

import play.api._
import mvc._
import data._
import http._

import scala.io.Codec
import com.codahale.jerkson.Json
import scalaz.effects.IO

trait LilaController extends Controller with ContentTypes {

  lazy val env = Global.env

  def JsonOk(map: Map[String, Any]) = Ok(Json generate map) as JSON

  def ValidOk(valid: Valid[Unit]) = valid.fold(
    e ⇒ BadRequest(e.list mkString "\n"),
    _ ⇒ Ok("ok")
  )

  def ValidIOk[A](form: Form[A])(op: A ⇒ IO[Unit])(implicit request: Request[_]) =
    form.bindFromRequest.fold(
      form ⇒ BadRequest(form.errors mkString "\n"),
      data ⇒ IOk(op(data))
    )

  def IOk(op: IO[Unit]) = Ok(op.unsafePerformIO)

  def get(name: String)(implicit request: Request[_]) =
    request.queryString get name flatMap (_.headOption)

  def getInt(name: String)(implicit request: Request[_]) =
    get(name)(request) map (_.toInt)

  // I like Unit requests.
  implicit def wUnit: Writeable[Unit] =
    Writeable[Unit](_ ⇒ Codec toUTF8 "ok")
  implicit def ctoUnit: ContentTypeOf[Unit] =
    ContentTypeOf[Unit](Some(ContentTypes.TEXT))

  implicit def richForm[A](form: Form[A]) = new {
    def toValid: Valid[A] = form.fold(
      form ⇒ failure(nel("Invalid form", form.errors.map(_.toString): _*)),
      data ⇒ success(data)
    )
  }
}
