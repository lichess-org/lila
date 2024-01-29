package lila.relay

import play.api.i18n.Lang

import lila.user.User
import lila.i18n.Language
import lila.memo.{ PicfitUrl, PicfitImage }

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
    spotlight: Option[RelayTour.Spotlight] = None,
    autoLeaderboard: Boolean = true,
    players: Option[RelayPlayers] = None,
    image: Option[PicfitImage.Id] = None
):
  inline def id = _id

  lazy val slug =
    val s = lila.common.String slugify name
    if s.isEmpty then "-" else s

  def withRounds(rounds: List[RelayRound]) = RelayTour.WithRounds(this, rounds)

  def official = tier.isDefined

  def reAssignIfOfficial = if official then copy(ownerId = User.broadcasterId) else this

  def path: String = s"/broadcast/$slug/$id"

  def tierIs(selector: RelayTour.Tier.type => RelayTour.Tier) =
    tier.fold(false)(_ == selector(RelayTour.Tier))

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
    val keys: Map[Tier, String] = Map(NORMAL -> "normal", HIGH -> "high", BEST -> "best")

  case class Spotlight(enabled: Boolean, language: Language, title: Option[String]):
    def isEmpty                           = !enabled && specialLanguage.isEmpty && title.isEmpty
    def specialLanguage: Option[Language] = language != lila.i18n.defaultLanguage option language

  case class WithRounds(tour: RelayTour, rounds: List[RelayRound])

  case class ActiveWithSomeRounds(tour: RelayTour, display: RelayRound, link: RelayRound)
      extends RelayRound.AndTour:
    export display.{ hasStarted as ongoing }

  case class WithLastRound(tour: RelayTour, round: RelayRound) extends RelayRound.AndTour:
    def link    = round
    def display = round

  object thumbnail:
    enum Size(val width: Int):
      def height = width / 2
      case Large extends Size(800)
      case Small extends Size(400)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: PicfitUrl, image: PicfitImage.Id, size: SizeSelector) =
      picfitUrl.thumbnail(image, size(thumbnail).width, size(thumbnail).height)

  import ornicar.scalalib.ThreadLocalRandom
  def makeId = Id(ThreadLocalRandom nextString 8)
