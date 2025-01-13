package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import io.mola.galimatias.URL
import java.time.ZoneId

import lila.core.i18n.Language
import lila.core.id.ImageId
import lila.core.misc.PicfitUrl
import lila.core.fide.FideTC
import lila.core.study.Visibility
import chess.TournamentClock
import chess.format.pgn.Tag

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
    showScores: Boolean = true,
    showRatingDiffs: Boolean = true,
    teamTable: Boolean = false,
    players: Option[RelayPlayersTextarea] = None,
    teams: Option[RelayTeamsTextarea] = None,
    image: Option[ImageId] = None,
    dates: Option[RelayTour.Dates] = None, // denormalized from round dates
    pinnedStream: Option[RelayPinnedStream] = None,
    note: Option[String] = None
):
  lazy val slug =
    val s = scalalib.StringOps.slug(name.value)
    if s.isEmpty then "-" else s

  def withRounds(rounds: List[RelayRound]) = RelayTour.WithRounds(this, rounds)

  def official = tier.isDefined

  def giveOfficialToBroadcasterIf(cond: Boolean) =
    if cond && official then copy(ownerId = UserId.broadcaster) else this

  def path: String = s"/broadcast/$slug/$id"

  def tierIs(selector: RelayTour.Tier.Selector) = tier.has(selector(RelayTour.Tier))

  def studyVisibility: Visibility =
    if tier.has(RelayTour.Tier.`private`)
    then Visibility.`private`
    else Visibility.public

object RelayTour:

  val maxRelays = Max(64)

  opaque type Name = String
  object Name extends OpaqueString[Name]

  enum Tier(val v: Int):
    case `private` extends Tier(-1)
    case normal    extends Tier(3)
    case high      extends Tier(4)
    case best      extends Tier(5)
    def key = toString

  object Tier:
    type Selector = RelayTour.Tier.type => RelayTour.Tier

    given cats.Order[Tier] = cats.Order.by(_.v)

    def apply(s: Selector) = s(Tier)

    val byV = values.mapBy(_.v)
    val options = List(
      ""                   -> "Non official",
      normal.v.toString    -> "Official: normal tier",
      high.v.toString      -> "Official: high tier",
      best.v.toString      -> "Official: best tier",
      `private`.v.toString -> "Private"
    )

  case class Info(
      format: Option[String],
      tc: Option[String],
      fideTc: Option[FideTC],
      location: Option[String],
      timeZone: Option[ZoneId],
      players: Option[String],
      website: Option[URL],
      standings: Option[URL]
  ):
    def nonEmpty          = List(format, tc, fideTc, location, players, website, standings).flatten.nonEmpty
    override def toString = List(format, tc, fideTc, location, players).flatten.mkString(" | ")
    lazy val fideTcOrGuess: FideTC     = fideTc | FideTC.standard
    def timeZoneOrDefault: ZoneId      = timeZone | ZoneId.systemDefault
    def clock: Option[TournamentClock] = tc.flatMap(TournamentClock.parse.apply)

  case class Dates(start: Instant, end: Option[Instant])

  case class Spotlight(enabled: Boolean, language: Language, title: Option[String]):
    def isEmpty                           = !enabled && specialLanguage.isEmpty && title.isEmpty
    def specialLanguage: Option[Language] = (language != lila.core.i18n.defaultLanguage).option(language)

  case class WithRounds(tour: RelayTour, rounds: List[RelayRound])

  case class WithLastRound(tour: RelayTour, round: RelayRound, group: Option[RelayGroup.Name])
      extends RelayRound.AndTourAndGroup:
    def link    = round
    def display = round

  case class WithFirstRound(tour: RelayTour, round: RelayRound, group: Option[RelayGroup.Name])
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
