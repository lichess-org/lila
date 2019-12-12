package lila.base

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Try

import com.typesafe.config.Config
import org.joda.time.{ DateTime, Duration }
import ornicar.scalalib.Zero
import scalaz._
import Scalaz._

import LilaTypes._

final class PimpedOption[A](private val self: Option[A]) extends AnyVal {

  import scalaz.std.{ option => o }

  def fold[X](some: A => X, none: => X): X = self.fold(none)(some)

  def |(a: => A): A = self getOrElse a

  def unary_~(implicit z: Zero[A]): A = self getOrElse z.zero
  def orDefault(implicit z: Zero[A]): A = self getOrElse z.zero

  def toSuccess[E](e: => E): scalaz.Validation[E, A] = o.toSuccess(self)(e)

  def toFailure[B](b: => B): scalaz.Validation[A, B] = o.toFailure(self)(b)

  def toTry(err: => Exception): Try[A] =
    self.fold[Try[A]](scala.util.Failure(err))(scala.util.Success.apply)

  def err(message: => String): A = self.getOrElse(sys.error(message))

  def ifNone(n: => Unit): Unit = if (self.isEmpty) n

  def has(a: A) = self contains a
}

final class PimpedString(private val s: String) extends AnyVal {

  def boot[A](v: => A): A = lila.common.Chronometer.syncEffect(v) { lap =>
    lila.log.boot.info(s"${lap.millis}ms $s")
  }

  def replaceIf(t: Char, r: Char): String =
    if (s.indexOf(t) >= 0) s.replace(t, r) else s

  def replaceIf(t: Char, r: CharSequence): String =
    if (s.indexOf(t) >= 0) s.replace(String.valueOf(t), r) else s

  def replaceIf(t: CharSequence, r: CharSequence): String =
    if (s.contains(t)) s.replace(t, r) else s
}

final class PimpedConfig(private val config: Config) extends AnyVal {

  def millis(name: String): Int = config.getDuration(name, TimeUnit.MILLISECONDS).toInt
  def seconds(name: String): Int = config.getDuration(name, TimeUnit.SECONDS).toInt
  def duration(name: String): FiniteDuration = millis(name).millis
}

final class PimpedDateTime(private val date: DateTime) extends AnyVal {
  def getSeconds: Long = date.getMillis / 1000
  def getCentis: Long = date.getMillis / 10
  def toNow = new Duration(date, DateTime.now)
}

final class PimpedValid[A](private val v: Valid[A]) extends AnyVal {

  def future: Fu[A] = v fold (errs => fufail(errs.shows), fuccess)
}

final class PimpedTry[A](private val v: Try[A]) extends AnyVal {

  def fold[B](fe: Exception => B, fa: A => B): B = v match {
    case scala.util.Failure(e: Exception) => fe(e)
    case scala.util.Failure(e) => throw e
    case scala.util.Success(a) => fa(a)
  }

  def future: Fu[A] = fold(Future.failed, fuccess)

  def toEither: Either[Throwable, A] = v match {
    case scala.util.Success(res) => Right(res)
    case scala.util.Failure(err) => Left(err)
  }
}

final class PimpedEither[A, B](private val v: Either[A, B]) extends AnyVal {
  import ornicar.scalalib.ValidTypes

  def toValid: Valid[B] = ValidTypes.eitherToValid(v)

  def orElse(other: => Either[A, B]): Either[A, B] = v match {
    case scala.util.Right(res) => Right(res)
    case scala.util.Left(_) => other
  }
}

final class PimpedFiniteDuration(private val d: FiniteDuration) extends AnyVal {

  def toCentis = chess.Centis {
    // divide by Double, then round, to avoid rounding issues with just `/10`!
    math.round {
      if (d.unit eq MILLISECONDS) d.length / 10d
      else d.toMillis / 10d
    }
  }

  def abs = if (d.length < 0) -d else d
}
