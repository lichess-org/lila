package lila

import ornicar.scalalib
import ornicar.scalalib.Zero
import org.joda.time.DateTime
import scala.util.Try

trait Steroids

  extends scalalib.Validation
  with scalalib.Common
  with scalalib.Regex
  with scalalib.OrnicarMonoid.Instances
  with scalalib.Zero.Syntax
  with scalalib.Zero.Instances
  with scalalib.OrnicarOption
  with scalalib.OrnicarNonEmptyList

  with scalaz.std.OptionInstances
  with scalaz.std.OptionFunctions
  with scalaz.syntax.std.ToOptionIdOps

  with scalaz.std.ListInstances
  with scalaz.std.ListFunctions
  with scalaz.syntax.std.ToListOps

  with scalaz.std.StringInstances

  with scalaz.std.TupleInstances

  with scalaz.syntax.ToIdOps
  with scalaz.syntax.ToEqualOps
  with scalaz.syntax.ToApplyOps
  with scalaz.syntax.ToValidationOps
  with scalaz.syntax.ToFunctorOps
  with scalaz.syntax.ToMonoidOps
  with scalaz.syntax.ToTraverseOps
  with scalaz.syntax.ToShowOps

  with LilaSteroids

trait LilaSteroids {
  import Wrappers._

  @inline implicit def toLilaPimpedOption[A](a: Option[A]) = new LilaPimpedOption(a)
  @inline implicit def toLilaPimpedTryList[A](l: List[Try[A]]) = new LilaPimpedTryList(l)
  @inline implicit def toLilaPimpedList[A](l: List[A]) = new LilaPimpedList(l)
  @inline implicit def toLilaPimpedSeq[A](l: List[A]) = new LilaPimpedSeq(l)
  @inline implicit def toLilaPimpedDateTime(d: DateTime) = new LilaPimpedDateTime(d)
  @inline implicit def toLilaPimpedBoolean(b: Boolean) = new LilaPimpedBoolean(b)
  @inline implicit def toLilaPimpedInt(i: Int) = new LilaPimpedInt(i)
  @inline implicit def toLilaPimpedFloat(f: Float) = new LilaPimpedFloat(f)
  @inline implicit def toLilaPimpedDouble(d: Double) = new LilaPimpedDouble(d)
  @inline implicit def toLilaPimpedByteArray(ba: Array[Byte]) = new LilaPimpedByteArray(ba)

  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

object Wrappers {
  final class LilaPimpedDateTime(private val date: DateTime) extends AnyVal {
    def getSeconds: Long = date.getMillis / 1000
    def getCentis: Long = date.getMillis / 10
  }

  final class LilaPimpedTryList[A](private val list: List[Try[A]]) extends AnyVal {
    def sequence: Try[List[A]] = (Try(List[A]()) /: list) {
      (a, b) => a flatMap (c => b map (d => d :: c))
    } map (_.reverse)
  }

  final class LilaPimpedList[A](private val list: List[A]) extends AnyVal {
    def sortLike[B](other: List[B], f: A => B): List[A] = list.sortWith {
      case (x, y) => other.indexOf(f(x)) < other.indexOf(f(y))
    }
  }

  final class LilaPimpedSeq[A](private val seq: Seq[A]) extends AnyVal {
    def has(a: A) = seq contains a
  }

  /*
   * Replaces scalaz boolean ops
   * so ?? works on Zero and not Monoid
   */
  final class LilaPimpedBoolean(private val self: Boolean) extends AnyVal {

    def ??[A](a: => A)(implicit z: Zero[A]): A = if (self) a else Zero[A].zero

    def !(f: => Unit) = if (self) f

    def fold[A](t: => A, f: => A): A = if (self) t else f

    def ?[X](t: => X) = new { def |(f: => X) = if (self) t else f }

    def option[A](a: => A): Option[A] = if (self) Some(a) else None
  }

  final class LilaPimpedInt(private val self: Int) extends AnyVal {

    def atLeast(bottomValue: Int): Int = self max bottomValue

    def atMost(topValue: Int): Int = self min topValue
  }

  final class LilaPimpedFloat(private val self: Float) extends AnyVal {

    def atLeast(bottomValue: Float): Float = self max bottomValue

    def atMost(topValue: Float): Float = self min topValue
  }

  final class LilaPimpedDouble(private val self: Double) extends AnyVal {

    def atLeast(bottomValue: Double): Double = self max bottomValue

    def atMost(topValue: Double): Double = self min topValue
  }

  final class LilaPimpedByteArray(private val self: Array[Byte]) extends AnyVal {
    def toBase64 = java.util.Base64.getEncoder.encodeToString(self)
  }

  /*
   * Replaces scalaz option ops
   * so ~ works on Zero and not Monoid
   */
  final class LilaPimpedOption[A](private val self: Option[A]) extends AnyVal {

    import scalaz.std.{ option => o }

    def fold[X](some: A => X, none: => X): X = self match {
      case None => none
      case Some(a) => some(a)
    }

    def |(a: => A): A = self getOrElse a

    def unary_~(implicit z: Zero[A]): A = self getOrElse z.zero
    def orDefault(implicit z: Zero[A]): A = self getOrElse z.zero

    def toSuccess[E](e: => E): scalaz.Validation[E, A] = o.toSuccess(self)(e)

    def toFailure[B](b: => B): scalaz.Validation[A, B] = o.toFailure(self)(b)

    def err(message: => String): A = self.getOrElse(sys.error(message))

    def ifNone(n: => Unit): Unit = if (self.isEmpty) n

    def has(a: A) = self contains a
  }
}
