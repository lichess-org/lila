package lila.relay

import org.joda.time.DateTime

import lila.user.User

case class RelayTour(
    _id: RelayTour.Id,
    name: String,
    description: String,
    markup: Option[lila.common.Markdown] = None,
    ownerId: User.ID,
    createdAt: DateTime,
    tier: Option[RelayTour.Tier], // if present, it's an official broadcast
    active: Boolean,              // a round is scheduled or ongoing
    syncedAt: Option[DateTime],   // last time a round was synced
    autoLeaderboard: Boolean = true
) {
  def id = _id

  lazy val slug = {
    val s = lila.common.String slugify name
    if (s.isEmpty) "-" else s
  }

  def withRounds(rounds: List[RelayRound]) = RelayTour.WithRounds(this, rounds)

  def official = tier.isDefined
}

object RelayTour {

  val maxRelays = 64

  case class Id(value: String) extends AnyVal with StringValue

  type Tier = Int
  object Tier {
    val NORMAL = 3
    val HIGH   = 4
    val BEST   = 5

    val options = List(
      ""              -> "Non official",
      NORMAL.toString -> "Official: normal tier",
      HIGH.toString   -> "Official: high tier",
      BEST.toString   -> "Official: best tier"
    )
    def name(tier: Tier) = options.collectFirst {
      case (t, n) if t == tier => n
    } | "???"
  }

  case class WithRounds(tour: RelayTour, rounds: List[RelayRound])

  case class ActiveWithNextRound(tour: RelayTour, round: RelayRound) extends RelayRound.AndTour {
    def ongoing = round.startedAt.isDefined
  }

  case class WithLastRound(tour: RelayTour, round: RelayRound) extends RelayRound.AndTour

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)
}
