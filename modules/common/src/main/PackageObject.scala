package lila

import ornicar.scalalib

import scalaz.{ Zero, Zeros, Functor, Monad, OptionT }
import scala.concurrent.Future

trait PackageObject
    extends WithFuture
    with scalalib.Validation
    with scalalib.Common
    with scalalib.Regex
    with scalalib.IO
    with scalalib.DateTime
    with scalaz.Identitys
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Zeros
    with scalaz.Booleans
    with scalaz.Options
    with scalaz.OptionTs {

  def !![A](msg: String): Valid[A] = msg.failNel[A]

  def nowMillis: Double = System.currentTimeMillis
  def nowSeconds: Int = (nowMillis / 1000).toInt

  lazy val logger = play.api.Logger("lila")
  def loginfo(s: String) { logger info s }
  def logwarn(s: String) { logger warn s }
  def logerr(s: String) { logger error s }
  def fuloginfo(s: String) = fuccess { loginfo(s) }
  def fulogwarn(s: String) = fuccess { logwarn(s) }
  def fulogerr(s: String) = fuccess { logerr(s) }

  implicit final class LilaPimpedOption[A](o: Option[A]) {

    def ??[B: Zero](f: A ⇒ B): B = o.fold(∅[B])(f)
  }

  implicit final class LilaPimpedMap[A, B](m: Map[A, B]) {

    def +?(bp: (Boolean, (A, B))): Map[A, B] = if (bp._1) m + bp._2 else m
  }

  implicit final class LilaPimpedString(s: String) {

    def describes[A](v: ⇒ A): A = { loginfo(s); v }
  }

  implicit final class LilaPimpedValid[A](v: Valid[A]) {

    def future: Fu[A] = v fold (errs ⇒ fufail(errs.shows), fuccess)
  }

  def parseIntOption(str: String): Option[Int] = try {
    Some(java.lang.Integer.parseInt(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  def parseFloatOption(str: String): Option[Float] = try {
    Some(java.lang.Float.parseFloat(str))
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  def intBox(in: Range.Inclusive)(v: Int): Int =
    math.max(in.start, math.min(v, in.end))

  def floatBox(in: Range.Inclusive)(v: Float): Float =
    math.max(in.start, math.min(v, in.end))
}

trait WithFuture extends Zeros with scalalib.Validation {

  import spray.util.pimps.PimpedFuture

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Exception, B](a: A): Fu[B] = Future failed a
  def fufail[A](a: String): Fu[A] = fufail(common.LilaException(a))
  def fufail[A](a: Failures): Fu[A] = fufail(common.LilaException(a))
  val funit = fuccess(())

  implicit def LilaFuZero[A: Zero]: Zero[Fu[A]] = zero(fuccess(∅[A]))

  implicit def SprayPimpedFuture[T](fut: Future[T]) = new PimpedFuture[T](fut)
}

trait WithPlay extends Zeros { self: PackageObject ⇒

  import play.api.libs.json._

  implicit def execontext = play.api.libs.concurrent.Execution.defaultContext

  // Typeclasses
  implicit val LilaFutureFunctor = new Functor[Fu] {
    def fmap[A, B](r: Fu[A], f: A ⇒ B) = r map f
  }

  implicit val LilaFutureMonad = new Monad[Fu] {
    def pure[A](a: ⇒ A) = fuccess(a)
    def bind[A, B](r: Fu[A], f: A ⇒ Fu[B]) = r flatMap f
  }

  implicit val LilaJsObjectZero: Zero[JsObject] = zero(JsObject(Seq.empty))

  implicit def LilaJsResultZero[A]: Zero[JsResult[A]] = zero(JsError(Seq.empty))

  implicit final class LilaPimpedFuture[A](fua: Fu[A]) {

    def >>-(sideEffect: ⇒ Unit): Funit = fua.void andThen {
      case _ ⇒ sideEffect
    }

    def >>[B](fub: ⇒ Fu[B]): Fu[B] = fua flatMap (_ ⇒ fub)

    def void: Funit = fua map (_ ⇒ Unit)

    def inject[B](b: B): Fu[B] = fua map (_ ⇒ b)

    def effectFold(fail: Exception ⇒ Unit, succ: A ⇒ Unit) {
      fua onComplete {
        case scala.util.Failure(e: Exception) ⇒ fail(e)
        case scala.util.Failure(e)            ⇒ throw e // Throwables
        case scala.util.Success(e)            ⇒ succ(e)
      }
    }

    def fold[B](fail: Exception ⇒ B, succ: A ⇒ B): Fu[B] =
      fua map succ recover { case e: Exception ⇒ fail(e) }

    def flatFold[B](fail: Exception ⇒ Fu[B], succ: A ⇒ Fu[B]): Fu[B] =
      fua flatMap succ recoverWith { case e: Exception ⇒ fail(e) }

    def logFailure(prefix: Throwable ⇒ String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception ⇒ logwarn(prefix(e) + " " + e.getMessage)
      case e            ⇒ throw e
    })
    def logFailure(prefix: ⇒ String): Fu[A] = fua ~ (_ onFailure {
      case e: Exception ⇒ logwarn(prefix + " " + e.getMessage)
      case e            ⇒ throw e
    })

    def addEffect(effect: A ⇒ Unit) = fua ~ (_ foreach effect)

    def thenPp: Fu[A] = fua ~ {
      _.effectFold(
        e ⇒ logwarn("[failure] " + e),
        a ⇒ loginfo("[success] " + a)
      )
    }
  }

  implicit final class LilaPimpedFutureOption[A](fua: Fu[Option[A]]) {

    def flatten(msg: ⇒ String): Fu[A] = fua flatMap {
      _.fold[Fu[A]](fufail(msg))(fuccess(_))
    }
  }

  implicit final class LilaPimpedBooleanForFuture(b: Boolean) {

    def optionFu[A](v: ⇒ Fu[A]): Fu[Option[A]] =
      if (b) v map (_.some) else fuccess(none)
  }

  object makeTimeout {

    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val short = seconds(1)
    implicit val large = seconds(5)
    implicit val veryLarge = minutes(5)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def seconds(s: Int): Timeout = Timeout(s.seconds)
    def minutes(m: Int): Timeout = Timeout(m.minutes)
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit): Funit = Future {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter ⇒ Unit): Funit =
    printToFile(new java.io.File(f))(op)
}
