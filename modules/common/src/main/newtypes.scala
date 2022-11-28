package lila.base

import org.joda.time.DateTime

// thanks Anton!
// https://github.com/indoorvivants/opaque-newtypes/blob/main/modules/core/src/main/scala/OpaqueNewtypes.scala
trait NewTypes:

  trait SameRuntime[A, T]:
    def apply(a: A): T

  object SameRuntime:
    def apply[A, T](f: A => T): SameRuntime[A, T] = new:
      def apply(a: A): T = f(a)

  type StringRuntime[A] = SameRuntime[A, String]
  type IntRuntime[A]    = SameRuntime[A, Int]
  type DoubleRuntime[A] = SameRuntime[A, Double]

  trait TotalWrapper[Newtype, Impl](using ev: Newtype =:= Impl):
    inline def raw(inline a: Newtype): Impl              = a.asInstanceOf[Impl]
    inline def apply(inline s: Impl): Newtype            = s.asInstanceOf[Newtype]
    inline def from[M[_]](inline f: M[Impl]): M[Newtype] = f.asInstanceOf[M[Newtype]]
    inline def from[M[_], B](using sr: SameRuntime[B, Impl])(inline f: M[B]): M[Newtype] =
      f.asInstanceOf[M[Newtype]]
    inline def from[M[_], B](inline other: TotalWrapper[B, Impl])(inline f: M[B]): M[Newtype] =
      f.asInstanceOf[M[Newtype]]

    given SameRuntime[Newtype, Impl] = new:
      def apply(a: Newtype): Impl = a.asInstanceOf[Impl]

    given SameRuntime[Impl, Newtype] = new:
      def apply(a: Impl): Newtype = a.asInstanceOf[Newtype]

    extension (a: Newtype)
      inline def value: Impl                                     = raw(a)
      inline def into[X](inline other: TotalWrapper[X, Impl]): X = other.apply(raw(a))
      inline def map(inline f: Impl => Impl): Newtype            = apply(f(raw(a)))
  end TotalWrapper

  trait OpaqueString[A](using A =:= String) extends TotalWrapper[A, String]

  trait OpaqueInt[A](using A =:= Int) extends TotalWrapper[A, Int]:
    extension (inline a: A)
      inline def unary_-                                               = apply(-raw(a))
      inline def >(inline o: Int): Boolean                             = raw(a) > o
      inline def <(inline o: Int): Boolean                             = raw(a) < o
      inline def >=(inline o: Int): Boolean                            = raw(a) >= o
      inline def <=(inline o: Int): Boolean                            = raw(a) <= o
      inline def +(inline o: Int): A                                   = apply(raw(a) + o)
      inline def -(inline o: Int): A                                   = apply(raw(a) - o)
      inline def atLeast(inline bot: Int): A                           = apply(math.max(raw(a), bot))
      inline def atMost(inline top: Int): A                            = apply(math.min(raw(a), top))
      inline def >[B](inline o: B)(using sr: IntRuntime[B]): Boolean   = >(sr(o))
      inline def <[B](inline o: B)(using sr: IntRuntime[B]): Boolean   = <(sr(o))
      inline def >=[B](inline o: B)(using sr: IntRuntime[B]): Boolean  = >=(sr(o))
      inline def <=[B](inline o: B)(using sr: IntRuntime[B]): Boolean  = <=(sr(o))
      inline def +[B](inline o: B)(using sr: IntRuntime[B]): A         = a + sr(o)
      inline def -[B](inline o: B)(using sr: IntRuntime[B]): A         = a - sr(o)
      inline def atLeast[B](inline bot: B)(using sr: IntRuntime[B]): A = atLeast(sr(bot))
      inline def atMost[B](inline top: B)(using sr: IntRuntime[B]): A  = atMost(sr(top))

  trait OpaqueLong[A](using A =:= Long) extends TotalWrapper[A, Long]
  trait OpaqueDouble[A](using A =:= Double) extends TotalWrapper[A, Double]:
    extension (inline a: A) inline def +(inline o: Int): A = apply(raw(a) + o)
  trait OpaqueFloat[A](using A =:= Float)   extends TotalWrapper[A, Float]
  trait OpaqueDate[A](using A =:= DateTime) extends TotalWrapper[A, DateTime]

  import scala.concurrent.duration.FiniteDuration
  trait OpaqueDuration[A](using A =:= FiniteDuration) extends TotalWrapper[A, FiniteDuration]

  abstract class YesNo[A](using ev: Boolean =:= A):
    val Yes: A = ev.apply(true)
    val No: A  = ev.apply(false)

    inline def from[M[_]](inline a: M[Boolean]): M[A] = a.asInstanceOf[M[A]]

    given SameRuntime[A, Boolean] = SameRuntime(_ == Yes)
    given SameRuntime[Boolean, A] = SameRuntime(if _ then Yes else No)

    inline def apply(inline b: Boolean): A = ev.apply(b)

    extension (inline a: A)
      inline def value: Boolean        = a == Yes
      inline def flip: A               = if value then No else Yes
      inline def yes: Boolean          = value
      inline def no: Boolean           = !value
      inline def &&(inline other: A)   = a.value && other.value
      inline def `||`(inline other: A) = a.value || other.value
  end YesNo

  inline def sameOrdering[A, T](using bts: SameRuntime[T, A], ord: Ordering[A]): Ordering[T] =
    Ordering.by(bts.apply(_))

  inline def stringOrdering[T: StringRuntime](using Ordering[String]): Ordering[T] = sameOrdering[String, T]
  inline def intOrdering[T: IntRuntime](using Ordering[Int]): Ordering[T]          = sameOrdering[Int, T]
  inline def doubleOrdering[T: DoubleRuntime](using Ordering[Double]): Ordering[T] = sameOrdering[Double, T]

  def stringIsString: StringRuntime[String] = new:
    def apply(a: String) = a
