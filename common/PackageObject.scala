package lila

import ornicar.scalalib

import scalaz.{ Zero, Functor, Monad, OptionT }
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
    with scalaz.OptionTs {

  val toVoid = (_: Any) ⇒ ()

  def !![A](msg: String): Valid[A] = msg.failNel[A]

  def nowMillis: Double = System.currentTimeMillis
  def nowSeconds: Int = (nowMillis / 1000).toInt

  implicit def richerMap[A, B](m: Map[A, B]) = new {
    def +?(bp: (Boolean, (A, B))): Map[A, B] = if (bp._1) m + bp._2 else m
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

  def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  def printToFile(f: String)(op: java.io.PrintWriter ⇒ Unit) {
    printToFile(new java.io.File(f))(op)
  }
}

trait WithFuture extends scalaz.Zeros {

  import spray.util.pimps.PimpedFuture

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def fuccess[A](a: A) = Future successful a
  def fufail[A <: Throwable](a: A) = Future failed a
  def funit = fuccess(())

  implicit def FuZero[A: Zero]: Zero[Fu[A]] = new Zero[Fu[A]] { val zero = fuccess(∅[A]) }

  implicit def pimpFuture[T](fut: Future[T]): PimpedFuture[T] = new PimpedFuture[T](fut)
}

trait WithDb { self: PackageObject ⇒

  // implicit def reactiveSortJsObject(sort: (String, SortOrder)): (String, JsValueWrapper) = sort match {
  //   case (field, SortOrder.Ascending) ⇒ field -> 1
  //   case (field, _)                   ⇒ field -> -1
  // }
}

trait WithPlay { self: PackageObject ⇒

  import play.api.libs.json._
  import play.api.libs.concurrent.Promise
  import play.api.libs.iteratee.{ Iteratee, Enumerator }
  import play.api.libs.iteratee.Concurrent.Channel
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits._

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type SocketFuture = Future[(Iteratee[JsValue, _], JsEnumerator)]

  // Typeclasses
  implicit def FutureFunctor = new Functor[Fu] {
    def fmap[A, B](r: Fu[A], f: A ⇒ B) = r map f
  }
  implicit def FutureMonad = new Monad[Fu] {
    def pure[A](a: ⇒ A) = fuccess(a)
    def bind[A, B](r: Fu[A], f: A ⇒ Fu[B]) = r flatMap f
  }

  implicit def futureOptionT[A](fuo: Fu[Option[A]]): OptionT[Future, A] = optionT(fuo)

  implicit def lilaRicherFuture[A](fua: Fu[A]) = new {

    def >>[B](fub: Fu[B]): Fu[B] = fua flatMap (_ ⇒ fub)

    def void: Funit = fua map (_ ⇒ Unit)

    def inject[B](b: B): Fu[B] = fua map (_ ⇒ b)
  }

  implicit def lilaRicherFutureZero[A: Zero](fua: Fu[A]) = new {

    def doIf(cond: Boolean): Fu[A] = cond.fold(fua, fuccess(∅[A]))

    def doUnless(cond: Boolean): Fu[A] = doIf(!cond)
  }

  implicit def pimpJsObject(obj: JsObject) = new {

    def get[T: Reads](field: String): Option[T] = (obj \ field) match {
      case JsUndefined(_) ⇒ none
      case value          ⇒ value.asOpt[T]
    }
  }
}
