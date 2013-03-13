package lila

import ornicar.scalalib

import scalaz.Zero
import scala.concurrent.Future

trait PackageObject
    extends scalalib.Validation
    with scalalib.Common
    with scalalib.Regex
    with scalalib.IO
    with scalalib.DateTime
    with scalaz.Identitys
    with scalaz.NonEmptyLists
    with scalaz.Strings
    with scalaz.Lists
    with scalaz.Zeros
    with scalaz.Booleans {

  type Fu[A] = Future[A]
  type Funit = Fu[Unit]

  def funit = fuccess(())
  def fuccess[A](a: A) = Future successful a

  implicit def FuZero[A: Zero]: Zero[Fu[A]] = new Zero[Fu[A]] { val zero = fuccess(∅[A]) }

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

trait WithDb { self: PackageObject ⇒

  type LilaDB = reactivemongo.api.DB

  type ReactiveColl = reactivemongo.api.collections.default.BSONCollection
}

trait WithPlay { self: PackageObject ⇒

  import play.api.libs.json.JsValue
  import play.api.libs.concurrent.Promise
  import play.api.libs.iteratee.{ Iteratee, Enumerator }
  import play.api.libs.iteratee.Concurrent.Channel
  import play.api.Play.current

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type SocketFuture = Future[(Iteratee[JsValue, _], JsEnumerator)]
}
