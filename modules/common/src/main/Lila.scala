package lila

object Lila extends Lila:

  // https://www.scala-lang.org/api/2.13.4/Executor%24.html#global:Executor
  val defaultExecutor: scala.concurrent.ExecutionContextExecutor =
    scala.concurrent.ExecutionContext.getClass
      .getDeclaredMethod("opportunistic")
      .invoke(scala.concurrent.ExecutionContext)
      .asInstanceOf[scala.concurrent.ExecutionContextExecutor]

  export ornicar.scalalib.newtypes.{ given, * }
  export ornicar.scalalib.zeros.given
  export ornicar.scalalib.extensions.{ given, * }
  export ornicar.scalalib.time.*

  export cats.syntax.all.*
  export cats.{ Eq, Show }
  export cats.data.NonEmptyList

  inline def nowNanos: Long  = System.nanoTime()
  inline def nowMillis: Long = System.currentTimeMillis()
  inline def nowCentis: Long = nowMillis / 10
  inline def nowTenths: Long = nowMillis / 100
  inline def nowSeconds: Int = (nowMillis / 1000).toInt

  object makeTimeout:

    import akka.util.Timeout
    import scala.concurrent.duration.*

    given short: Timeout = apply(1.second)
    given long: Timeout  = apply(5.seconds)

    def apply(duration: FiniteDuration) = akka.util.Timeout(duration)

  def some[A](a: A): Option[A] = Some(a)

trait Lila
    extends lila.base.LilaTypes
    with lila.base.LilaModel
    with lila.base.LilaUserId
    with cats.syntax.OptionSyntax
    with cats.syntax.ListSyntax
    with lila.base.LilaLibraryExtensions
    with lila.base.JsonExtensions:

  trait StringValue extends Any:
    def value: String
    override def toString = value

  // replaces Product.unapply in play forms
  def unapply[P <: Product](p: P)(using m: scala.deriving.Mirror.ProductOf[P]): Option[m.MirroredElemTypes] =
    Some(Tuple.fromProductTyped(p))

  // move somewhere else when we have more Eqs
  given cats.Eq[play.api.i18n.Lang] = cats.Eq.fromUniversalEquals
