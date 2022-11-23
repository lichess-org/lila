package lila.base

// thanks Anton!
// https://github.com/indoorvivants/opaque-newtypes/blob/main/modules/core/src/main/scala/OpaqueNewtypes.scala
trait NewTypes:

  trait SameRuntime[A, T]:
    def apply(a: A): T

  object SameRuntime:
    def apply[A, T](f: A => T): SameRuntime[A, T] = new:
      def apply(a: A): T = f(a)

  type StringRuntime[A] = SameRuntime[A, String]

  trait TotalWrapper[Newtype, Impl](using ev: Newtype =:= Impl):
    inline def raw(inline a: Newtype): Impl         = a.asInstanceOf[Impl]
    inline def apply(inline s: Impl): Newtype       = s.asInstanceOf[Newtype]
    inline def from(a: Array[Impl]): Array[Newtype] = a.asInstanceOf[Array[Newtype]]
    inline def from(a: List[Impl]): List[Newtype]   = a.asInstanceOf[List[Newtype]]

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
  trait OpaqueInt[A](using A =:= Int)       extends TotalWrapper[A, Int]
  trait OpaqueLong[A](using A =:= Long)     extends TotalWrapper[A, Long]
  trait OpaqueDouble[A](using A =:= Double) extends TotalWrapper[A, Double]
  trait OpaqueFloat[A](using A =:= Float)   extends TotalWrapper[A, Float]

  import scala.concurrent.duration.FiniteDuration
  trait OpaqueDuration[A](using A =:= FiniteDuration) extends TotalWrapper[A, FiniteDuration]

  abstract class YesNo[A](using ev: Boolean =:= A):
    val Yes: A = ev.apply(true)
    val No: A  = ev.apply(false)

    given SameRuntime[A, Boolean] = SameRuntime(_ == Yes)
    given SameRuntime[Boolean, A] = SameRuntime(if _ then Yes else No)

    inline def apply(inline b: Boolean): A = ev.apply(b)

    extension (inline a: A)
      inline def value: Boolean        = a == Yes
      inline def flip: A               = if value then No else Yes
      inline def &&(inline other: A)   = a.value && other.value
      inline def `||`(inline other: A) = a.value || other.value
  end YesNo

  inline def sameOrdering[A, T](using bts: SameRuntime[T, A], ord: Ordering[A]): Ordering[T] =
    Ordering.by(bts.apply(_))

  inline def stringOrdering[T](using SameRuntime[T, String], Ordering[String]): Ordering[T] =
    sameOrdering[String, T]

  def stringIsString: StringRuntime[String] = new:
    def apply(a: String) = a
