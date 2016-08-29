package lila

import ornicar.scalalib
import ornicar.scalalib.Zero

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

  with BooleanSteroids
  with OptionSteroids
  with ListSteroids

  with JodaTimeSteroids

trait JodaTimeSteroids {
  import org.joda.time.DateTime
  implicit final class LilaPimpedDateTime(date: DateTime) {
    def getSeconds: Long = date.getMillis / 1000
    def getDate: java.util.Date = date.toDate
  }
  implicit val dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

trait ListSteroids {

  import scala.util.{ Try, Success }

  implicit final class LilaPimpedTryList[A](list: List[Try[A]]) {
    def sequence: Try[List[A]] = (Try(List[A]()) /: list) {
      (a, b) => a flatMap (c => b map (d => d :: c))
    } map (_.reverse)
  }
  implicit final class LilaPimpedList[A](list: List[A]) {
    def sortLike[B](other: List[B], f: A => B): List[A] = list.sortWith {
      case (x, y) => other.indexOf(f(x)) < other.indexOf(f(y))
    }
  }
}

trait BooleanSteroids {

  /*
   * Replaces scalaz boolean ops
   * so ?? works on Zero and not Monoid
   */
  implicit final class LilaPimpedBoolean(self: Boolean) {

    def ??[A](a: => A)(implicit z: Zero[A]): A = if (self) a else Zero[A].zero

    def !(f: => Unit) = if (self) f

    def fold[A](t: => A, f: => A): A = if (self) t else f

    def ?[X](t: => X) = new { def |(f: => X) = if (self) t else f }

    def option[A](a: => A): Option[A] = if (self) Some(a) else None
  }
}

trait IntSteroids {

  implicit final class LilaPimpedInt(self: Int) {

    def atLeast(bottomValue: Int): Int = self max bottomValue

    def atMost(topValue: Int): Int = self min topValue
  }
}

trait OptionSteroids {

  /*
   * Replaces scalaz option ops
   * so ~ works on Zero and not Monoid
   */
  implicit final class LilaPimpedOption[A](self: Option[A]) {

    import scalaz.std.{ option => o }

    def fold[X](some: A => X, none: => X): X = self match {
      case None    => none
      case Some(a) => some(a)
    }

    def |(a: => A): A = self getOrElse a

    def unary_~(implicit z: Zero[A]): A = self getOrElse z.zero

    def toSuccess[E](e: => E): scalaz.Validation[E, A] = o.toSuccess(self)(e)

    def toFailure[B](b: => B): scalaz.Validation[A, B] = o.toFailure(self)(b)

    def err(message: => String): A = self.getOrElse(sys.error(message))

    def ifNone(n: => Unit): Unit = if (self.isEmpty) n
  }
}
