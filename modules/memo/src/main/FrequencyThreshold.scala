package lila.memo

import com.github.blemale.scaffeine.Scaffeine

final class FrequencyThreshold[K](count: Int, duration: FiniteDuration):

  private val cache = Scaffeine()
    .expireAfter[K, Int](
      create = (_, _) => duration,
      update = (_, _, current) => current,
      read = (_, _, current) => current
    )
    .build[K, Int]()

  private val concMap = cache.underlying.asMap()

  /* Returns true when called more than `count` times in `duration` window. */
  def apply(key: K): Boolean = concMap.compute(
    key,
    (_, prev) => Option(prev).fold(1)(_ + 1)
  ) >= count
