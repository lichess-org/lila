package lila.core
package data

case class Strings(value: List[String]) extends AnyVal
case class UserIds(value: List[UserId]) extends AnyVal
case class Ints(value: List[Int])       extends AnyVal

case class Template(value: String) extends AnyVal

opaque type Days = Int
object Days extends OpaqueInt[Days]

opaque type Seconds = Int
object Seconds extends OpaqueInt[Seconds]

case class Preload[A](value: Option[A]) extends AnyVal:
  def orLoad(f: => Fu[A]): Fu[A] = value.fold(f)(fuccess)
object Preload:
  def apply[A](value: A): Preload[A] = Preload(value.some)
  def none[A]                        = Preload[A](None)

final class LazyFu[A](run: () => Fu[A]):
  lazy val value: Fu[A]             = run()
  def dmap[B](f: A => B): LazyFu[B] = LazyFu(() => value.dmap(f))
object LazyFu:
  def sync[A](v: => A): LazyFu[A] = LazyFu(() => fuccess(v))

case class CircularDep[A](resolve: () => A)
