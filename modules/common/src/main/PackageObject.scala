package lila

trait PackageObject extends Lilaisms:

  def nowNanos: Long  = System.nanoTime()
  def nowMillis: Long = System.currentTimeMillis()
  def nowCentis: Long = nowMillis / 10
  def nowTenths: Long = nowMillis / 100
  def nowSeconds: Int = (nowMillis / 1000).toInt

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
