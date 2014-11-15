package lila.tournament

import org.joda.time.DateTime

// Metadata about running tournaments: who got byed, when a round completed, this sort of things.
sealed abstract class Event(val id: Int) {
  def timestamp: DateTime
  def encode: RawEvent
}

case class RoundEnd(timestamp: DateTime) extends Event(1) {
  def encode = RawEvent(id, timestamp, None)
}

case class Bye(user: String, timestamp: DateTime) extends Event(10) {
  def encode = RawEvent(id, timestamp, Some(user))
}

private[tournament] case class RawEvent(
  i: Int,
  t: DateTime,
  u: Option[String]) {

  def decode: Option[Event] = roundEnd orElse bye

  def roundEnd: Option[RoundEnd] = (i == 1) option RoundEnd(t)

  def bye: Option[Bye] = for {
    usr <- u
    if i == 10
  } yield Bye(usr, t)
}
