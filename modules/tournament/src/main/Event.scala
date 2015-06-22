package lila.tournament

import org.joda.time.DateTime

// Metadata about running tournaments: who got byed, when a round completed, this sort of things.
sealed abstract class Event(val id: Int) {
  def timestamp: DateTime
}

case class RoundEnd(timestamp: DateTime) extends Event(1)

case class Bye(user: String, timestamp: DateTime) extends Event(10)
