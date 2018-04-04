package lila.tournament

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import org.joda.time.DateTime
import scala.concurrent.duration._

/*
 * Computes the delay before a player can rejoin a tournament after pausing.
 * The first pause results in a delay of gameTotalTime / 5 (e.g. 60 seconds for 5+0)
 * with a minimum of 30 seconds and a maximum of 180 seconds.
 * Following pauses accumulate delays (2x delay, 3x delay, etc...).
 * After 20 minutes without a pause, the delay is reinitialized.
 */
private final class Pauser {

  import Pauser._

  private val cache: Cache[Player.ID, Pauses] = Scaffeine()
    .expireAfterWrite(20 minutes)
    .build[Player.ID, Pauses]

  private def getPauses(player: Player) = cache.get(player.id, _ => Pauses(0))

  private def initialDelayOf(tour: Tournament) = Delay {
    (tour.clock.estimateTotalSeconds / 5) atLeast 30 atMost 120
  }

  def add(player: Player, tour: Tournament): Delay = {
    val pauses = getPauses(player).add
    initialDelayOf(tour) * pauses
  }
}

object Pauser {

  // pause counter of a player
  case class Pauses(count: Int) extends AnyVal {
    def add = copy(count = count + 1)
  }

  // pause counter of a player
  case class Delay(seconds: Int) extends AnyVal {
    def *(pauses: Pauses) = copy(seconds = seconds * pauses.count)
  }
}
