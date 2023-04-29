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

  inline def nowNanos: Long  = System.nanoTime()
  inline def nowMillis: Long = System.currentTimeMillis()
  inline def nowCentis: Long = nowMillis / 10
  inline def nowTenths: Long = nowMillis / 100
  inline def nowSeconds: Int = (nowMillis / 1000).toInt

  object makeTimeout:

    import akka.util.Timeout
    import scala.concurrent.duration.*

    given short: Timeout  = seconds(1)
    given large: Timeout  = seconds(5)
    given larger: Timeout = seconds(30)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def millis(s: Int): Timeout         = Timeout(s.millis)
    def seconds(s: Int): Timeout        = Timeout(s.seconds)
    def minutes(m: Int): Timeout        = Timeout(m.minutes)

  def some[A](a: A): Option[A] = Some(a)

trait Lila
    extends lila.base.LilaTypes
    with lila.base.LilaModel
    with lila.base.LilaUserId
    with cats.syntax.OptionSyntax
    with cats.syntax.ListSyntax
    with lila.base.LilaLibraryExtensions:

  trait StringValue extends Any:
    def value: String
    override def toString = value

  // replaces Product.unapply in play forms
  def unapply[P <: Product](p: P)(using m: scala.deriving.Mirror.ProductOf[P]): Option[m.MirroredElemTypes] =
    Some(Tuple.fromProductTyped(p))

  import play.api.libs.json.{ JsObject, JsValue }
  import lila.base.{ LilaJsObject, LilaJsValue }
  // can't use extensions because of method name shadowing :(
  implicit def toLilaJsObject(jo: JsObject): LilaJsObject = LilaJsObject(jo)
  implicit def toLilaJsValue(jv: JsValue): LilaJsValue    = LilaJsValue(jv)
