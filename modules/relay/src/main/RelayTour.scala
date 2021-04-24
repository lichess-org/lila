package lila.relay

import org.joda.time.DateTime

import lila.user.User

case class RelayTour(
    _id: RelayTour.Id,
    name: String,
    description: String,
    markup: Option[String] = None,
    ownerId: User.ID,
    official: Boolean,
    createdAt: DateTime
) {
  def id = _id

  def slug = {
    val s = lila.common.String slugify name
    if (s.isEmpty) "-" else s
  }

  def withRounds(rounds: List[RelayRound]) = RelayTour.WithRounds(this, rounds)
}

object RelayTour {

  val maxRelays = 64

  case class Id(value: String) extends AnyVal with StringValue

  case class WithRounds(tour: RelayTour, rounds: List[RelayRound])

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)
}
