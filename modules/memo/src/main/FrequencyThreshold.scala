package lila.memo

import scala.concurrent.duration.FiniteDuration
import com.github.blemale.scaffeine.Scaffeine

final class FrequencyThreshold[K](count: Int, duration: FiniteDuration) {

  private val cache = Scaffeine().expireAfterWrite(duration).build[K, Int]()

  private val concMap = cache.underlying.asMap()

  def apply(key: K): Boolean = concMap.compute(
    key,
    (_, prev) => Option(prev).fold(1)(_ + 1)
  ) >= count
}
