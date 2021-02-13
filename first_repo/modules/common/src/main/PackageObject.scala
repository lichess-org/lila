package lila

trait PackageObject extends Lilaisms {

  def nowNanos: Long  = System.nanoTime()
  def nowMillis: Long = System.currentTimeMillis()
  def nowCentis: Long = nowMillis / 10
  def nowTenths: Long = nowMillis / 100
  def nowSeconds: Int = (nowMillis / 1000).toInt

  type ~[A, B] = Tuple2[A, B]
  object ~ {
    def apply[A, B](x: A, y: B)                              = Tuple2(x, y)
    def unapply[A, B](x: Tuple2[A, B]): Option[Tuple2[A, B]] = Some(x)
  }

  object makeTimeout {

    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val short     = seconds(1)
    implicit val large     = seconds(5)
    implicit val larger    = seconds(30)
    implicit val veryLarge = minutes(5)

    implicit val halfSecond = millis(500)

    def apply(duration: FiniteDuration) = Timeout(duration)
    def millis(s: Int): Timeout         = Timeout(s.millis)
    def seconds(s: Int): Timeout        = Timeout(s.seconds)
    def minutes(m: Int): Timeout        = Timeout(m.minutes)
  }

  def some[A](a: A): Option[A] = Some(a)
}
