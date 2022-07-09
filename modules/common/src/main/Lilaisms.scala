package lila

import cats.data.Validated
import com.typesafe.config.Config
import org.joda.time.DateTime
import ornicar.scalalib
import play.api.libs.json.{ JsObject, JsValue }
import scala.concurrent.duration._
import scala.util.Try

import lila.base._

trait Lilaisms
    extends LilaTypes
    with scalalib.Common
    with scalalib.OrnicarOption
    with scalalib.Regex
    with scalalib.Zeros
    with cats.syntax.OptionSyntax
    with cats.syntax.ListSyntax {

  type StringValue = lila.base.LilaTypes.StringValue
  type IntValue    = lila.base.LilaTypes.IntValue
  type Percent     = lila.base.LilaTypes.Percent

  @inline implicit def toLilaFuture[A](f: Fu[A])               = new LilaFuture(f)
  @inline implicit def toLilaFutureBoolean(f: Fu[Boolean])     = new LilaFutureBoolean(f)
  @inline implicit def toLilaFutureOption[A](f: Fu[Option[A]]) = new LilaFutureOption(f)
  @inline implicit def toLilaIterableFuture[A, M[X] <: IterableOnce[X]](t: M[Fu[A]]) =
    new LilaIterableFuture(t)

  @inline implicit def toLilaJsObject(jo: JsObject) = new LilaJsObject(jo)
  @inline implicit def toLilaJsValue(jv: JsValue)   = new LilaJsValue(jv)

  @inline implicit def toAugmentedAny[A](b: A)   = new AugmentedAny(b)
  @inline implicit def toLilaBoolean(b: Boolean) = new LilaBoolean(b)
  @inline implicit def toLilaInt(i: Int)         = new LilaInt(i)
  @inline implicit def toLilaLong(l: Long)       = new LilaLong(l)
  @inline implicit def toLilaFloat(f: Float)     = new LilaFloat(f)
  @inline implicit def toLilaDouble(d: Double)   = new LilaDouble(d)

  @inline implicit def toLilaTryList[A](l: List[Try[A]]) = new LilaTryList(l)
  @inline implicit def toLilaList[A](l: List[A])         = new LilaList(l)
  @inline implicit def toLilaSeq[A](l: Seq[A])           = new LilaSeq(l)
  @inline implicit def toLilaByteArray(ba: Array[Byte])  = new LilaByteArray(ba)

  @inline implicit def toLilaOption[A](a: Option[A])           = new LilaOption(a)
  @inline implicit def toLilaString(s: String)                 = new LilaString(s)
  @inline implicit def toLilaConfig(c: Config)                 = new LilaConfig(c)
  @inline implicit def toLilaDateTime(d: DateTime)             = new LilaDateTime(d)
  @inline implicit def toLilaTry[A](t: Try[A])                 = new LilaTry(t)
  @inline implicit def toLilaEither[A, B](e: Either[A, B])     = new LilaEither(e)
  @inline implicit def toLilaFiniteDuration(d: FiniteDuration) = new LilaFiniteDuration(d)

  @inline implicit def toRichValidated[E, A](v: Validated[E, A]) = new RichValidated(v)
}
