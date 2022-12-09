package lila.base

import scala.concurrent.duration.Duration
import scala.concurrent.Future

import org.joda.time.DateTime
import alleycats.Zero
import play.api.libs.json.{ JsError, JsObject, JsResult }

trait LilaTypes:

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A): Fu[A]        = Future.successful(a)
  def fufail[X](t: Throwable): Fu[X] = Future.failed(t)
  def fufail[X](s: String): Fu[X]    = fufail(LilaException(s))
  val funit                          = fuccess(())
  val fuTrue                         = fuccess(true)
  val fuFalse                        = fuccess(false)

  given Zero[Funit] with
    def zero = funit
  given Zero[Fu[Boolean]] with
    def zero = fuFalse
  given [A](using az: Zero[A]): Zero[Fu[A]] with
    def zero = fuccess(az.zero)

  given Zero[Duration] with
    def zero = Duration.Zero
  given Zero[JsObject] with
    def zero = JsObject(Seq.empty)
