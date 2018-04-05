package lila.tournament

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.joda.time.DateTime
import scala.concurrent.duration._

/*
 * Computes the delay before a player can rejoin a tournament after pausing.
 * The first pause results in a delay of gameTotalTime / 5 (e.g. 60 seconds for 5+0)
 * with a minimum of 30 seconds and a maximum of 120 seconds.
 * Following pauses accumulate delays (2x delay, 3x delay, etc...).
 * After 20 minutes without a pause, the delay is reinitialized.
 */
private final class Pauser {

  import Pauser._

  private val cache: Cache[Player.ID, Record] = Scaffeine()
    .expireAfterWrite(20 minutes)
    .build[Player.ID, Record]

  private def initialDelayOf(tour: Tournament) = Delay {
    (tour.clock.estimateTotalSeconds / 5) atLeast 30 atMost 120
  }

  def add(player: Player, tour: Tournament): Delay = {
    val record = cache.getIfPresent(player.id).fold(Record(1, DateTime.now))(_.add)
    initialDelayOf(tour) * record
  }

  def remainingDelay(player: Player, tour: Tournament): Option[Delay] =
    cache.getIfPresent(player.id) flatMap { record =>
      val seconds = record.pausedAt.getSeconds - nowSeconds + (initialDelayOf(tour) * record).seconds
      seconds > 1 option Delay(seconds.toInt)
    }
}

object Pauser {

  case class Record(count: Int, pausedAt: DateTime) {
    def add = copy(
      count = count + 1,
      pausedAt = DateTime.now
    )
  }

  // pause counter of a player
  case class Delay(seconds: Int) extends AnyVal {
    def *(record: Record) = copy(seconds = seconds * record.count)
    def isEmpty = seconds < 1
  }
}
