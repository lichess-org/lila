package lila
package memo

import scalaz.effects._

final class MonoMemo[A](ttl: Int, f: IO[A]) {

  def apply: A = {
    if (expired) refresh
    value
  }

  private var value: A = _
  private var ts: Double = 0

  private def expired = nowMillis > ts + ttl
  private def refresh {
    value = f.unsafePerformIO
    ts = nowMillis
  }
}
