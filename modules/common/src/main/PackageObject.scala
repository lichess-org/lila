package lila

import ornicar.scalalib

import scalaz.{ Zero, Functor, Monad, OptionT }
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

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

  implicit final class LilaPimpedOption[A](o: Option[A]) {

    def zmap[B: Zero](f: A ⇒ B): B = o.fold(∅[B])(f)
  }

  implicit final class LilaPimpedMap[A, B](m: Map[A, B]) {

    def +?(bp: (Boolean, (A, B))): Map[A, B] = if (bp._1) m + bp._2 else m
  }

  implicit final class LilaPimpedString(s: String) {

    def describes[A](v: ⇒ A): A = { loginfo(s); v }
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

  def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit): Funit = Future {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter ⇒ Unit): Funit =
    printToFile(new java.io.File(f))(op)
}

trait WithFuture extends scalaz.Zeros {

  import spray.util.pimps.PimpedFuture

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Throwable, B](a: A): Fu[B] = Future failed a
  def fufail[B](a: String): Fu[B] = Future failed (new RuntimeException(a))
  val funit = fuccess(())

  implicit def LilaFuZero[A: Zero] = new Zero[Fu[A]] { val zero = fuccess(∅[A]) }

  implicit def SprayPimpedFuture[T](fut: Future[T]) = new PimpedFuture[T](fut)
}

trait WithPlay { self: PackageObject ⇒

  import play.api.libs.json._
  import play.api.libs.concurrent.Promise
  import play.api.libs.iteratee.{ Iteratee, Enumerator }
  import play.api.libs.iteratee.Concurrent.Channel
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits._

  // Typeclasses
  implicit val LilaFutureFunctor = new Functor[Fu] {
    def fmap[A, B](r: Fu[A], f: A ⇒ B) = r map f
  }
  implicit val LilaFutureMonad = new Monad[Fu] {
    def pure[A](a: ⇒ A) = fuccess(a)
    def bind[A, B](r: Fu[A], f: A ⇒ Fu[B]) = r flatMap f
  }
  implicit val LilaJsObjectZero = new Zero[JsObject] { val zero = JsObject(Seq.empty) }

  implicit def LilaJsResultZero[A] = new Zero[JsResult[A]] {
    val zero = JsError(Seq.empty)
  }

  implicit final class LilaPimpedFuture[A](fua: Fu[A]) {

    def >>(sideEffect: ⇒ Unit): Funit = >>(fuccess(sideEffect))

    def >>[B](fub: Fu[B]): Fu[B] = fua flatMap (_ ⇒ fub)

    def void: Funit = fua map (_ ⇒ Unit)

    def inject[B](b: B): Fu[B] = fua map (_ ⇒ b)
  }

  implicit final class LilaPimpedFutureZero[A: Zero](fua: Fu[A]) {

    def doIf(cond: Boolean): Fu[A] = cond.fold(fua, fuccess(∅[A]))

    def doUnless(cond: Boolean): Fu[A] = doIf(!cond)
  }

  object makeTimeout {

    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val short = seconds(1)
    implicit val large = seconds(5)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def seconds(s: Int): Timeout = Timeout(s.seconds)
    def minutes(m: Int): Timeout = Timeout(m.minutes)
  }
}
