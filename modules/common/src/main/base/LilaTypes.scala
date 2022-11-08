package lila.base

import scala.concurrent.duration.Duration
import scala.concurrent.Future

import org.joda.time.DateTime
import alleycats.Zero
import play.api.libs.json.{ JsError, JsResult, JsObject }

trait LilaTypes {

  trait IntValue extends Any {
    def value: Int
    override def toString = value.toString
  }
  trait BooleanValue extends Any {
    def value: Boolean
    override def toString = value.toString
  }
  trait DoubleValue extends Any {
    def value: Double
    override def toString = value.toString
  }

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  @inline def fuccess[A](a: A): Fu[A] = Future.successful(a)
  def fufail[X](t: Throwable): Fu[X]  = Future.failed(t)
  def fufail[X](s: String): Fu[X]     = fufail(LilaException(s))
  val funit                           = fuccess(())
  val fuTrue                          = fuccess(true)
  val fuFalse                         = fuccess(false)

  given Zero[Funit] with 
    def zero = Zero(funit)
  given Zero[Fu[Boolean]] with 
    def zero = Zero(fuFalse)
  given [A](using az: Zero[A]): Zero[Fu[A]] with
    def zero = fuccess(az.zero)

  given Zero[Duration] with
    def zero = Duration.Zero
  given Zero[JsObject] with
    def zero = JsObject(Seq.empty)
  // given Zero[JsResult] with
  //   def zero = JsError(Seq.empty)

  given Ordering[DateTime] with
    def zero = Ordering.fromLessThan(_ isBefore _)
}

object LilaTypes extends LilaTypes {

  trait StringValue extends Any {
    def value: String
    override def toString = value
  }

  trait Percent extends Any {
    def value: Double
    def toInt = Math.round(value).toInt // round to closest
  }
}
