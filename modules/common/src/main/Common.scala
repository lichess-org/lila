package lila

object Common extends Common:
  // global execution context that doubles as an executor
  // see ExecutionContextOpportunistic.scala
  given defaultExecutor: scala.concurrent.ExecutionContextExecutor =
    scala.concurrent.ExecutionContextOpportunistic

  export scalalib.newtypes.{ given, * }
  export scalalib.zeros.given
  export scalalib.extensions.{ given, * }
  export scalalib.time.*

  export cats.syntax.all.*
  export cats.{ Eq, Show }
  export cats.data.NonEmptyList

  inline def nowNanos: Long  = System.nanoTime()
  inline def nowMillis: Long = System.currentTimeMillis()
  inline def nowCentis: Long = nowMillis / 10
  inline def nowTenths: Long = nowMillis / 100
  inline def nowSeconds: Int = (nowMillis / 1000).toInt

  def some[A](a: A): Option[A] = Some(a)

trait Common
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
  given cats.Show[StringValue] = cats.Show.show(_.value)

  // replaces Product.unapply in play forms
  def unapply[P <: Product](p: P)(using m: scala.deriving.Mirror.ProductOf[P]): Option[m.MirroredElemTypes] =
    Some(Tuple.fromProductTyped(p))

  // move somewhere else when we have more Eqs
  given cats.Eq[play.api.i18n.Lang] = cats.Eq.fromUniversalEquals

  import play.api.Mode
  extension (mode: Mode)
    def isDev   = mode == Mode.Dev
    def isProd  = mode == Mode.Prod
    def notProd = mode != Mode.Prod
