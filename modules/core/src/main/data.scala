package lila.core

import scalalib.newtypes.OpaqueString

import scala.concurrent.ExecutionContext

import lila.core.userId.UserId

// has to be an object, not a package,
// to makes sure opaque types don't leak out
object data:

  case class Strings(value: List[String]) extends AnyVal
  case class UserIds(value: List[UserId]) extends AnyVal
  case class Ints(value: List[Int])       extends AnyVal

  trait OpaqueInstant[A](using A =:= Instant) extends TotalWrapper[A, Instant]

  opaque type RichText = String
  object RichText extends OpaqueString[RichText]

  opaque type Markdown = String
  object Markdown extends OpaqueString[Markdown]

  opaque type Html = String
  object Html extends OpaqueString[Html]:
    def apply(frag: scalatags.Text.Frag): Html = frag.render

  opaque type JsonStr = String
  object JsonStr extends OpaqueString[JsonStr]

  // JSON string that is safe to include in HTML
  opaque type SafeJsonStr = String
  object SafeJsonStr extends OpaqueString[SafeJsonStr]

  opaque type Template = String
  object Template extends OpaqueString[Template]

  case class Preload[A](value: Option[A]) extends AnyVal:
    def orLoad(f: => Fu[A]): Fu[A] = value.fold(f)(Future.successful)
  object Preload:
    def apply[A](value: A): Preload[A] = Preload(value.some)
    def none[A]                        = Preload[A](None)

  final class LazyFu[A](run: () => Fu[A]):
    lazy val value: Fu[A]             = run()
    def dmap[B](f: A => B): LazyFu[B] = LazyFu(() => value.map(f)(using ExecutionContext.parasitic))
  object LazyFu:
    def sync[A](v: => A): LazyFu[A] = LazyFu(() => Future.successful(v))

  final class CircularDep[A](val resolve: () => A)

  final class SimpleMemo[A](ttl: Option[FiniteDuration])(compute: () => A):
    private var value: A                     = compute()
    private var recomputeAt: Option[Instant] = ttl.map(nowInstant.plus(_))
    def get(): A =
      if recomputeAt.exists(_.isBeforeNow) then
        recomputeAt = ttl.map(nowInstant.plus(_))
        value = compute()
      value
