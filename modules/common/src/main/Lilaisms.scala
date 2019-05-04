package lila

import scala.util.Try
import scala.concurrent.duration._

import ornicar.scalalib
import org.joda.time.DateTime
import com.typesafe.config.Config
import play.api.libs.json.{ JsObject, JsValue }
import lila.base._

trait Lilaisms
  extends LilaTypes
  with scalalib.Common
  with scalalib.OrnicarNonEmptyList
  with scalalib.OrnicarOption
  with scalalib.OrnicarMonoids
  with scalalib.Regex
  with scalalib.Validation
  with scalalib.Zeros
  with scalalib.Zero.Syntax
  with scalaz.std.ListFunctions
  with scalaz.std.ListInstances
  with scalaz.std.OptionFunctions
  with scalaz.std.OptionInstances
  with scalaz.std.StringInstances
  with scalaz.std.TupleInstances
  with scalaz.syntax.std.ToListOps
  with scalaz.syntax.std.ToOptionIdOps
  with scalaz.syntax.ToApplyOps
  with scalaz.syntax.ToEqualOps
  with scalaz.syntax.ToFunctorOps
  with scalaz.syntax.ToIdOps
  with scalaz.syntax.ToMonoidOps
  with scalaz.syntax.ToShowOps
  with scalaz.syntax.ToTraverseOps
  with scalaz.syntax.ToValidationOps {

  type StringValue = lila.base.LilaTypes.StringValue

  @inline implicit def toPimpedFuture[A](f: Fu[A]) = new PimpedFuture(f)
  @inline implicit def toPimpedFutureBoolean(f: Fu[Boolean]) = new PimpedFutureBoolean(f)
  @inline implicit def toPimpedFutureOption[A](f: Fu[Option[A]]) = new PimpedFutureOption(f)
  @inline implicit def toPimpedFutureValid[A](f: Fu[Valid[A]]) = new PimpedFutureValid(f)
  @inline implicit def toPimpedTraversableFuture[A, M[X] <: TraversableOnce[X]](t: M[Fu[A]]) =
    new PimpedTraversableFuture(t)

  @inline implicit def toPimpedJsObject(jo: JsObject) = new PimpedJsObject(jo)
  @inline implicit def toPimpedJsValue(jv: JsValue) = new PimpedJsValue(jv)

  @inline implicit def toPimpedBoolean(b: Boolean) = new PimpedBoolean(b)
  @inline implicit def toPimpedInt(i: Int) = new PimpedInt(i)
  @inline implicit def toPimpedLong(l: Long) = new PimpedLong(l)
  @inline implicit def toPimpedFloat(f: Float) = new PimpedFloat(f)
  @inline implicit def toPimpedDouble(d: Double) = new PimpedDouble(d)

  @inline implicit def toPimpedTryList[A](l: List[Try[A]]) = new PimpedTryList(l)
  @inline implicit def toPimpedList[A](l: List[A]) = new PimpedList(l)
  @inline implicit def toPimpedSeq[A](l: Seq[A]) = new PimpedSeq(l)
  @inline implicit def toPimpedChars(i: Iterable[CharSequence]) = new PimpedChars(i)
  @inline implicit def toPimpedByteArray(ba: Array[Byte]) = new PimpedByteArray(ba)

  @inline implicit def toPimpedOption[A](a: Option[A]) = new PimpedOption(a)
  @inline implicit def toPimpedString(s: String) = new PimpedString(s)
  @inline implicit def toPimpedConfig(c: Config) = new PimpedConfig(c)
  @inline implicit def toPimpedDateTime(d: DateTime) = new PimpedDateTime(d)
  @inline implicit def toPimpedValid[A](v: Valid[A]) = new PimpedValid(v)
  @inline implicit def toPimpedTry[A](t: Try[A]) = new PimpedTry(t)
  @inline implicit def toPimpedEither[A, B](e: Either[A, B]) = new PimpedEither(e)
  @inline implicit def toPimpedFiniteDuration(d: FiniteDuration) = new PimpedFiniteDuration(d)

  @inline implicit def toPimpedActorSystem(a: akka.actor.ActorSystem) = new PimpedActorSystem(a)
}

final class PimpedActorSystem(private val a: akka.actor.ActorSystem) extends AnyVal {
  def lilaBus = lila.common.Bus(a)
}
