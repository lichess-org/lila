package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.i18n.Language
import lila.core.id.ImageId
import lila.core.misc.PicfitUrl

case class RelayTour(
    @Key("_id") id: RelayTourId,
    name: RelayTour.Name,
    info: RelayTour.Info,
    markup: Option[Markdown] = None,
    ownerId: UserId,
    createdAt: Instant,
    tier: Option[RelayTour.Tier], // if present, it's an official broadcast
    active: Boolean,              // a round is scheduled or ongoing
    live: Option[Boolean],        // a round is live, i.e. started and not finished
    syncedAt: Option[Instant],    // last time a round was synced
    spotlight: Option[RelayTour.Spotlight] = None,
    autoLeaderboard: Boolean = true,
    teamTable: Boolean = false,
    players: Option[RelayPlayersTextarea] = None,
    teams: Option[RelayTeamsTextarea] = None,
    image: Option[ImageId] = None,
    dates: Option[RelayTour.Dates] = None, // denormalized from round dates
    pinnedStream: Option[RelayPinnedStream] = None
):
  lazy val slug =
    val s = scalalib.StringOps.slug(name.value)
    if s.isEmpty then "-" else s

  def withRounds(rounds: List[RelayRound]) = RelayTour.WithRounds(this, rounds)

  def official = tier.isDefined

  def giveOfficialToBroadcasterIf(cond: Boolean) =
    if cond && official then copy(ownerId = UserId.broadcaster) else this

  def path: String = s"/broadcast/$slug/$id"

  def tierIs(selector: RelayTour.Tier.Selector) =
    tier.fold(false)(_ == selector(RelayTour.Tier))

object RelayTour:

  val maxRelays = 64

  opaque type Name = String
  object Name extends OpaqueString[Name]

  type Tier = Int
  object Tier:
    val PRIVATE = -1
    val NORMAL  = 3
    val HIGH    = 4
    val BEST    = 5

    val options = List(
      ""               -> "Non official",
      NORMAL.toString  -> "Official: normal tier",
      HIGH.toString    -> "Official: high tier",
      BEST.toString    -> "Official: best tier",
      PRIVATE.toString -> "Private"
    )
    def name(tier: Tier) = options.collectFirst {
      case (t, n) if t == tier.toString => n
    } | "???"
    val keys: Map[Tier, String] = Map(
      NORMAL  -> "normal",
      HIGH    -> "high",
      BEST    -> "best",
      PRIVATE -> "private"
    )
    type Selector = RelayTour.Tier.type => RelayTour.Tier

  case class Info(
      format: Option[String],
      tc: Option[String],
      players: Option[String]
  ):
    val all = List(format, tc, players).flatten
    export all.nonEmpty
    override def toString = all.mkString(" | ")

  case class Dates(start: Instant, end: Option[Instant])

  case class Spotlight(enabled: Boolean, language: Language, title: Option[String]):
    def isEmpty                           = !enabled && specialLanguage.isEmpty && title.isEmpty
    def specialLanguage: Option[Language] = (language != lila.core.i18n.defaultLanguage).option(language)

  case class WithRounds(tour: RelayTour, rounds: List[RelayRound])

  case class ActiveWithSomeRounds(
      tour: RelayTour,
      display: RelayRound, // which round to show on the tour link
      link: RelayRound,    // which round to actually link to
      group: Option[RelayGroup.Name]
  ) extends RelayRound.AndTourAndGroup:
    def errors: List[String] =
      val round = display
      ~round.sync.log.lastErrors.some
        .filter(_.nonEmpty)
        .orElse:
          (round.hasStarted && round.sync.upstream.isDefined && !round.sync.ongoing)
            .option(List("Not syncing!"))

  case class WithLastRound(tour: RelayTour, round: RelayRound, group: Option[RelayGroup.Name])
      extends RelayRound.AndTourAndGroup:
    def link    = round
    def display = round

  case class IdName(@Key("_id") id: RelayTourId, name: Name)

  case class WithGroup(tour: RelayTour, group: Option[RelayGroup])
  case class WithGroupTours(tour: RelayTour, group: Option[RelayGroup.WithTours])

  object thumbnail:
    enum Size(val width: Int, aspectRatio: Float = 2.0f):
      def height: Int = (width / aspectRatio).toInt
      case Large     extends Size(800)
      case Small     extends Size(400)
      case Small16x9 extends Size(400, 16.0f / 9)
    type SizeSelector = thumbnail.type => Size

    def apply(picfitUrl: PicfitUrl, image: ImageId, size: SizeSelector) =
      picfitUrl.thumbnail(image, size(thumbnail).width, size(thumbnail).height)

  import scalalib.ThreadLocalRandom
  def makeId = RelayTourId(ThreadLocalRandom.nextString(8))
