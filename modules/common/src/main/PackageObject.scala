package lila

import scala.util.Try

import scalaz.{ Monad, Monoid, OptionT, ~> }

trait PackageObject extends Lilaisms {
  implicit lazy val playExecutionContext = play.api.libs.concurrent.Execution.defaultContext

  def !![A](msg: String): Valid[A] = msg.failureNel[A]

  def nowNanos: Long = System.nanoTime()
  def nowMillis: Long = System.currentTimeMillis()
  def nowCentis: Long = nowMillis / 10
  def nowTenths: Long = nowMillis / 100
  def nowSeconds: Int = (nowMillis / 1000).toInt

  // from scalaz. We don't want to import all OptionTFunctions, because of the clash with `some`
  def optionT[M[_]] = new (({ type λ[α] = M[Option[α]] })#λ ~> ({ type λ[α] = OptionT[M, α] })#λ) {
    def apply[A](a: M[Option[A]]) = new OptionT[M, A](a)
  }

  implicit def fuMonoid[A: Monoid]: Monoid[Fu[A]] =
    Monoid.instance((x, y) => x zip y map {
      case (a, b) => a ⊹ b
    }, fuccess(∅[A]))

  implicit lazy val monadFu = new Monad[Fu] {
    override def map[A, B](fa: Fu[A])(f: A => B) = fa map f
    def point[A](a: => A) = fuccess(a)
    def bind[A, B](fa: Fu[A])(f: A => Fu[B]) = fa flatMap f
  }

  type ~[+A, +B] = Tuple2[A, B]
  object ~ {
    def apply[A, B](x: A, y: B) = Tuple2(x, y)
    def unapply[A, B](x: Tuple2[A, B]): Option[Tuple2[A, B]] = Some(x)
  }

  def parseIntOption(str: String): Option[Int] =
    Try(java.lang.Integer.parseInt(str)).toOption

  def parseFloatOption(str: String): Option[Float] =
    Try(java.lang.Float.parseFloat(str)).toOption

  def parseLongOption(str: String): Option[Long] =
    Try(java.lang.Long.parseLong(str)).toOption

  def parseDoubleOption(str: String): Option[Double] =
    Try(java.lang.Double.parseDouble(str)).toOption

  def intBox(in: Range.Inclusive)(v: Int): Int =
    math.max(in.start, math.min(v, in.end))

  def floatBox(in: Range.Inclusive)(v: Float): Float =
    math.max(in.start, math.min(v, in.end))

  def doubleBox(in: Range.Inclusive)(v: Double): Double =
    math.max(in.start, math.min(v, in.end))

  object makeTimeout {

    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val short = seconds(1)
    implicit val large = seconds(5)
    implicit val larger = seconds(30)
    implicit val veryLarge = minutes(5)

    implicit val halfSecond = millis(500)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def millis(s: Int): Timeout = Timeout(s.millis)
    def seconds(s: Int): Timeout = Timeout(s.seconds)
    def minutes(m: Int): Timeout = Timeout(m.minutes)
  }
}
