package draughts

case class Timestamp(value: Long)
  extends AnyVal with Ordered[Timestamp] {

  def -(o: Timestamp) = Centis.ofMillis(value - o.value)

  def +(o: Centis) = Timestamp(value + o.millis)

  def compare(other: Timestamp) = value compare other.value
}

trait Timestamper {
  def now: Timestamp

  def toNow(ts: Timestamp) = now - ts
}

private[draughts] object RealTimestamper extends Timestamper {
  def now = new Timestamp(System.currentTimeMillis)
}