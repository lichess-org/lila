package lila.base

import alleycats.Zero
import cats.data.Validated
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import org.joda.time.{ DateTime, Duration }
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.matching.Regex
import scala.util.Try

import cats.data.NonEmptyList
import java.util.Base64

trait LilaLibraryExtensions extends LilaTypes:

  extension [A](self: Option[A])

    def fold[X](some: A => X, none: => X): X = self.fold(none)(some)

    def orDefault(implicit z: Zero[A]): A = self getOrElse z.zero

    def toTryWith(err: => Exception): Try[A] =
      self.fold[Try[A]](scala.util.Failure(err))(scala.util.Success.apply)

    def toTry(err: => String): Try[A] = toTryWith(lila.base.LilaException(err))

    def err(message: => String): A = self.getOrElse(sys.error(message))

    def has(a: A) = self contains a

  extension (s: String)

    def replaceIf(t: Char, r: Char): String =
      if (s.indexOf(t.toInt) >= 0) s.replace(t, r) else s

    def replaceIf(t: Char, r: CharSequence): String =
      if (s.indexOf(t.toInt) >= 0) s.replace(String.valueOf(t), r) else s

    def replaceIf(t: CharSequence, r: CharSequence): String =
      if (s.contains(t)) s.replace(t, r) else s

    def replaceAllIn(regex: Regex, replacement: String) = regex.replaceAllIn(s, replacement)

  extension (config: Config)
    def millis(name: String): Int              = config.getDuration(name, TimeUnit.MILLISECONDS).toInt
    def seconds(name: String): Int             = config.getDuration(name, TimeUnit.SECONDS).toInt
    def duration(name: String): FiniteDuration = millis(name).millis

  extension (date: DateTime)
    def getSeconds: Long         = date.getMillis / 1000
    def getCentis: Long          = date.getMillis / 10
    def toNow                    = new Duration(date, DateTime.now)
    def atMost(other: DateTime)  = if (other isBefore date) other else date
    def atLeast(other: DateTime) = if (other isAfter date) other else date

  extension [A](v: Try[A])

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

  extension [A, B](v: Either[A, B])
    def orElse(other: => Either[A, B]): Either[A, B] =
      v match {
        case scala.util.Right(res) => Right(res)
        case scala.util.Left(_)    => other
      }

  extension (d: FiniteDuration)
    def toCentis = chess.Centis {
      // divide by Double, then round, to avoid rounding issues with just `/10`!
      math.round {
        if (d.unit eq MILLISECONDS) d.length / 10d
        else d.toMillis / 10d
      }
    }
    def abs = if (d.length < 0) -d else d

  extension [E, A](v: Validated[E, A]) def toFuture: Fu[A] = v.fold(err => fufail(err.toString), fuccess)

  extension [A](list: List[Try[A]]) def sequence: Try[List[A]] = Try(list map { _.get })

  extension [A](list: List[A])
    def sortLike[B](other: List[B], f: A => B): List[A] =
      list.sortWith { (x, y) =>
        other.indexOf(f(x)) < other.indexOf(f(y))
      }
    def toNel: Option[NonEmptyList[A]] =
      list match {
        case Nil           => None
        case first :: rest => Some(NonEmptyList(first, rest))
      }
    def tailOption: Option[List[A]] = list match {
      case Nil       => None
      case _ :: rest => Some(rest)
    }
    def tailSafe: List[A]         = tailOption getOrElse Nil
    def indexOption(a: A)         = Option(list indexOf a).filter(0 <=)
    def previous(a: A): Option[A] = indexOption(a).flatMap(i => list.lift(i - 1))
    def next(a: A): Option[A]     = indexOption(a).flatMap(i => list.lift(i + 1))

  extension [A](seq: Seq[A]) def has(a: A) = seq contains a

  extension (self: Array[Byte]) def toBase64 = Base64.getEncoder.encodeToString(self)
