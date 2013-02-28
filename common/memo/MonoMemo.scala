package lila.common.memo

import scalaz.effects._

final class MonoMemo[A](ttl: Int, f: IO[A]) {

  def apply: A = {
    if (expired) refresh
    value
  }

  private var value: A = _
  private var ts: Double = 0

  def refresh {
    value = f.unsafePerformIO
    ts = nowMillis
  }

  private def expired = nowMillis > ts + ttl
}
