package controllers

import lila.http._

import play.api._
import mvc._
import http._

import scala.io.Codec
import com.codahale.jerkson.Json
import scalaz.effects.IO

trait LilaController extends Controller {

  val env = new HttpEnv(Play.unsafeApplication.configuration.underlying)

  val json = "application/json"

  def JsonOk(map: Map[String, Any]) = Ok(Json generate map) as json

  def ValidOk(valid: Valid[Unit]) = valid.fold(
    e ⇒ BadRequest(e.list mkString "\n"),
    _ ⇒ Ok("ok")
  )

  def IOk(op: IO[Unit]) = Ok(op.unsafePerformIO)

  // I like Unit requests.
  implicit def wUnit: Writeable[Unit] =
    Writeable[Unit](_ ⇒ Codec toUTF8 "ok")
  implicit def ctoUnit: ContentTypeOf[Unit] =
    ContentTypeOf[Unit](Some(ContentTypes.TEXT))
}
