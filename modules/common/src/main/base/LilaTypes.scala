package lila.base

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import play.api.libs.json.JsError
import play.api.libs.json.JsObject

import alleycats.Zero
import org.joda.time.DateTime

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

  implicit val fUnitZero: Zero[Fu[Unit]]       = Zero(funit)
  implicit val fBooleanZero: Zero[Fu[Boolean]] = Zero(fuFalse)

  implicit def fuZero[A](implicit az: Zero[A]): Zero[Fu[A]] =
    new Zero[Fu[A]] {
      def zero = fuccess(az.zero)
    }

  implicit val durationZero: Zero[Duration] = Zero(Duration.Zero)
  implicit val jsObjectZero: Zero[JsObject] = Zero(JsObject(Seq.empty))
  implicit val jsResultZero: Zero[JsError]  = Zero(JsError(Seq.empty))

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

object LilaTypes extends LilaTypes {

  trait StringValue extends Any {
    def value: String
    override def toString = value
  }
}
