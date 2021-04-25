package lila.relay

import org.joda.time.DateTime

import lila.user.User

case class RelayTour(
    _id: RelayTour.Id,
    name: String,
    description: String,
    markup: Option[String] = None,
    ownerId: User.ID,
    createdAt: DateTime,
    official: Boolean,
    active: Boolean,           // a round is scheduled or ongoing
    syncedAt: Option[DateTime] // last time a round was synced
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

  case class ActiveWithNextRound(tour: RelayTour, round: RelayRound) {
    def path    = RelayRound.WithTour(round, tour).path
    def ongoing = round.startedAt.isDefined
  }

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)
}
