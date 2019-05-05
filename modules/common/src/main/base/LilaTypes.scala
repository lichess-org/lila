package lila.base

import scala.concurrent.duration.Duration
import scala.concurrent.Future

import org.joda.time.DateTime
import ornicar.scalalib.{ Zero, ValidTypes }
import play.api.libs.json.{ JsObject, JsError }

trait LilaTypes extends ValidTypes {

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
  def fufail[X](t: Throwable): Fu[X] = Future.failed(t)
  def fufail[X](s: String): Fu[X] = fufail(LilaException(s))
  def fufail[X](f: Failures): Fu[X] = fufail(LilaException(f))
  val funit = fuccess(())
  val fuTrue = fuccess(true)
  val fuFalse = fuccess(false)

  implicit val fUnitZero: Zero[Fu[Unit]] = Zero.instance(funit)
  implicit val fBooleanZero: Zero[Fu[Boolean]] = Zero.instance(fuFalse)

  implicit def fuZero[A](implicit az: Zero[A]) = new Zero[Fu[A]] {
    def zero = fuccess(az.zero)
  }

  implicit val durationZero: Zero[Duration] = Zero.instance(Duration.Zero)
  implicit val jsObjectZero = Zero.instance(JsObject(Seq.empty))
  implicit val jsResultZero = Zero.instance(JsError(Seq.empty))

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

object LilaTypes extends LilaTypes {

  trait StringValue extends Any {
    def value: String
    override def toString = value
  }
}
