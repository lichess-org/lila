package lila.relay

import ornicar.scalalib.ThreadLocalRandom

import lila.user.User

case class RelayTour(
    _id: RelayTour.Id,
    name: String,
    description: String,
    markup: Option[Markdown] = None,
    ownerId: UserId,
    createdAt: Instant,
    tier: Option[RelayTour.Tier], // if present, it's an official broadcast
    active: Boolean,              // a round is scheduled or ongoing
    syncedAt: Option[Instant],    // last time a round was synced
    autoLeaderboard: Boolean = true,
    players: Option[RelayPlayers] = None
):
  inline def id = _id

  lazy val slug =
    val s = lila.common.String slugify name
    if (s.isEmpty) "-" else s

  def withRounds(rounds: List[RelayRound]) = RelayTour.WithRounds(this, rounds)

  def official = tier.isDefined

  def reAssignIfOfficial = if (official) copy(ownerId = User.broadcasterId) else this

object RelayTour:

  val maxRelays = 64

  opaque type Id = String
  object Id extends OpaqueString[Id]

  type Tier = Int
  object Tier:
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
      case (t, n) if t == tier.toString => n
    } | "???"

  case class WithRounds(tour: RelayTour, rounds: List[RelayRound])

  case class ActiveWithNextRound(tour: RelayTour, round: RelayRound) extends RelayRound.AndTour:
    def ongoing = round.startedAt.isDefined

  case class WithLastRound(tour: RelayTour, round: RelayRound) extends RelayRound.AndTour

  def makeId = Id(ThreadLocalRandom nextString 8)
