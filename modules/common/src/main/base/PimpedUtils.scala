package lila.base

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import scala.util.matching.Regex

import alleycats.Zero
import cats.data.Validated
import com.typesafe.config.Config
import org.joda.time.DateTime
import org.joda.time.Duration

import lila.base.LilaTypes._

final class PimpedOption[A](private val self: Option[A]) extends AnyVal {

  def fold[X](some: A => X, none: => X): X = self.fold(none)(some)

  def orDefault(implicit z: Zero[A]): A = self getOrElse z.zero

  def toTryWith(err: => Exception): Try[A] =
    self.fold[Try[A]](scala.util.Failure(err))(scala.util.Success.apply)

  def toTry(err: => String): Try[A] = toTryWith(lila.base.LilaException(err))

  def err(message: => String): A = self.getOrElse(sys.error(message))

  def ifNone(n: => Unit): Unit = if (self.isEmpty) n

  def has(a: A) = self contains a

  def ??[B: Zero](f: A => B): B = self.fold(Zero[B].zero)(f)

  def ifTrue(b: Boolean): Option[A]  = self filter (_ => b)
  def ifFalse(b: Boolean): Option[A] = self filter (_ => !b)

  // typesafe getOrElse
  def |(default: => A): A = self getOrElse default

  def unary_~(implicit z: Zero[A]): A = self getOrElse z.zero
}

final class PimpedString(private val s: String) extends AnyVal {

  def replaceIf(t: Char, r: Char): String =
    if (s.indexOf(t.toInt) >= 0) s.replace(t, r) else s

  def replaceIf(t: Char, r: CharSequence): String =
    if (s.indexOf(t.toInt) >= 0) s.replace(String.valueOf(t), r) else s

  def replaceIf(t: CharSequence, r: CharSequence): String =
    if (s.contains(t)) s.replace(t, r) else s
}

final class PimpedConfig(private val config: Config) extends AnyVal {

  def millis(name: String): Int              = config.getDuration(name, TimeUnit.MILLISECONDS).toInt
  def seconds(name: String): Int             = config.getDuration(name, TimeUnit.SECONDS).toInt
  def duration(name: String): FiniteDuration = millis(name).millis
}

final class PimpedDateTime(private val date: DateTime) extends AnyVal {
  def getSeconds: Long         = date.getMillis / 1000
  def getCentis: Long          = date.getMillis / 10
  def toNow                    = new Duration(date, DateTime.now)
  def atMost(other: DateTime)  = if (other isBefore date) other else date
  def atLeast(other: DateTime) = if (other isAfter date) other else date
}

final class PimpedTry[A](private val v: Try[A]) extends AnyVal {

  def fold[B](fe: Exception => B, fa: A => B): B =
    v match {
      case scala.util.Failure(e: Exception) => fe(e)
      case scala.util.Failure(e)            => throw e
      case scala.util.Success(a)            => fa(a)
    }

  def future: Fu[A] = fold(Future.failed, fuccess)

  def toEither: Either[Throwable, A] =
    v match {
      case scala.util.Success(res) => Right(res)
      case scala.util.Failure(err) => Left(err)
    }
}

final class PimpedEither[A, B](private val v: Either[A, B]) extends AnyVal {

  def orElse(other: => Either[A, B]): Either[A, B] =
    v match {
      case scala.util.Right(res) => Right(res)
      case scala.util.Left(_)    => other
    }
}

final class PimpedFiniteDuration(private val d: FiniteDuration) extends AnyVal {

  def toCentis =
    shogi.Centis {
      // divide by Double, then round, to avoid rounding issues with just `/10`!
      math.round {
        if (d.unit eq MILLISECONDS) d.length / 10d
        else d.toMillis / 10d
      }
    }

  def abs = if (d.length < 0) -d else d
}

final class PimpedRegex(private val r: Regex) extends AnyVal {

  def find(s: String): Boolean =
    r.pattern.matcher(s).find

  def matches(s: String): Boolean =
    r.pattern.matcher(s).matches
}

final class RichValidated[E, A](private val v: Validated[E, A]) extends AnyVal {

  def toFuture: Fu[A] = v.fold(err => fufail(err.toString), fuccess)

  def flatMap[EE >: E, B](f: A => Validated[EE, B]): Validated[EE, B] = v.andThen(f)
}

final class AddKcombinator[A](private val any: A) extends AnyVal {
  def kCombinator(sideEffect: A => Unit): A = {
    sideEffect(any)
    any
  }
  def ~(sideEffect: A => Unit): A = kCombinator(sideEffect)
  def pp: A                       = kCombinator(println)
  def pp(msg: String): A          = kCombinator(a => println(s"[$msg] $a"))
}
