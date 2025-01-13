package lila.tournament

/*
 * Computes the delay before a player can rejoin a tournament after pausing.
 * The first pause results in a delay of 10 seconds.
 * Next delays durations increase linearly as `pauses * gameTotalTime / 15`
 * (e.g. 20 seconds for second pause in 5+0) with maximum of 120 seconds.
 * After 20 minutes without any pause, the delay is reinitialized to 10s.
 */
final private class Pause(using Executor):

  import Pause.*

  private val cache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(20.minutes)
    .build[UserId, Record]()

  private def baseDelayOf(tour: Tournament) = Delay:
    tour.clock.estimateTotalSeconds / 15

  private def delayOf(record: Record, tour: Tournament) =
    // 10s for first pause
    // next ones increasing linearly until 120s
    baseDelayOf(tour).map: delay =>
      (delay * (record.pauses - 1)).atLeast(10).atMost(120)

  def add(userId: UserId): Unit =
    cache.put(
      userId,
      cache.getIfPresent(userId).fold(newRecord)(_.add)
    )

  def remainingDelay(userId: UserId, tour: Tournament): Option[Delay] =
    cache.getIfPresent(userId).flatMap { record =>
      val seconds = record.pausedAt.toSeconds - nowSeconds + delayOf(record, tour).value
      (seconds > 1).option(Delay(seconds.toInt))
    }

  def canJoin(userId: UserId, tour: Tournament): Boolean =
    tour.isCreated || remainingDelay(userId, tour).isEmpty

object Pause:

  case class Record(pauses: Int, pausedAt: Instant):
    def add = copy(
      pauses = pauses + 1,
      pausedAt = nowInstant
    )
  val newRecord = Record(1, nowInstant)

  // pause counter of a player
  opaque type Delay = Int
  object Delay extends TotalWrapper[Delay, Int]
