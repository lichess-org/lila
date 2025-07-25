package lila.relay

import io.mola.galimatias.URL
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom
import scalalib.model.Seconds
import chess.{ Rated, ByColor }

import lila.study.Study

case class RelayRound(
    /* Same as the Study id it refers to */
    @Key("_id") id: RelayRoundId,
    tourId: RelayTourId,
    name: RelayRound.Name,
    caption: Option[RelayRound.Caption],
    sync: RelayRound.Sync,
    /* When it's planned to start */
    startsAt: Option[RelayRound.Starts],
    /* When it actually starts */
    startedAt: Option[Instant],
    /* at least it *looks* finished... but maybe it's not
     * sync.nextAt is used for actually synchronising */
    finishedAt: Option[Instant],
    createdAt: Instant,
    crowd: Option[Crowd],
    // crowdAt: Option[Instant], // in DB but not used by RelayRound
    rated: Rated = Rated.Yes,
    customScoring: Option[ByColor[RelayRound.CustomScoring]] = none
):
  inline def studyId = id.into(StudyId)

  lazy val slug =
    val s = scalalib.StringOps.slug(name.value)
    if s.isEmpty then "-" else s

  def isFinished = finishedAt.isDefined

  def startsAtTime = startsAt.flatMap:
    case RelayRound.Starts.At(at) => at.some
    case _ => none
  def startsAfterPrevious = startsAt.contains(RelayRound.Starts.AfterPrevious)

  def finish =
    copy(
      finishedAt = finishedAt.orElse(nowInstant.some),
      sync = sync.pause
    )

  def resume(official: Boolean) =
    copy(
      finishedAt = none,
      sync = sync.play(official)
    )

  def ensureStarted = copy(startedAt = startedAt.orElse(nowInstant.some))
  def hasStarted = startedAt.isDefined
  def hasStartedEarly = startedAt.exists(at => startsAtTime.exists(_.isAfter(at)))
  def shouldHaveStarted = hasStarted || startsAtTime.exists(_.isBefore(nowInstant))

  def shouldHaveStarted1Hour = !hasStarted && startsAtTime.exists(_.isBefore(nowInstant.minusHours(1)))

  def looksStalled =
    hasStarted && !isFinished && sync.log.updatedAt.forall(_.isBefore(nowInstant.minusHours(1)))

  def shouldGiveUp =
    !hasStarted && startsAtTime.match
      case Some(at) => at.isBefore(nowInstant.minusHours(3))
      case None => createdAt.isBefore(nowInstant.minusDays(1))

  def withSync(f: Update[RelayRound.Sync]) = copy(sync = f(sync))

  def withTour(tour: RelayTour) = RelayRound.WithTour(this, tour)

  override def toString = s"""relay #$id "$name" $sync"""

object RelayRound:

  def makeId = RelayRoundId(ThreadLocalRandom.nextString(8))

  opaque type Order = Int
  object Order extends OpaqueInt[Order]

  opaque type Name = String
  object Name extends OpaqueString[Name]

  opaque type Caption = String
  object Caption extends OpaqueString[Caption]

  opaque type CustomPoints = Float
  object CustomPoints extends OpaqueFloat[CustomPoints]

  case class CustomScoring(win: CustomPoints, draw: CustomPoints)

  enum Starts:
    case At(at: Instant)
    case AfterPrevious

  case class Sync(
      upstream: Option[Sync.Upstream], // if empty, needs a client to push PGN
      until: Option[Instant], // sync until then; resets on move
      nextAt: Option[Instant], // when to run next sync
      period: Option[Seconds], // override time between two sync (rare)
      delay: Option[Seconds], // add delay between the source and the study
      onlyRound: Option[Sync.OnlyRound], // only keep games with [Round "x"]
      slices: Option[List[RelayGame.Slice]] = none,
      log: SyncLog
  ):
    def hasUpstream = upstream.isDefined
    def isPush = upstream.isEmpty

    def ongoing = until.so(_.isAfterNow)

    def play(official: Boolean) =
      if hasUpstream
      then
        copy(
          until = nowInstant.plusHours(if official then 3 else 1).some,
          nextAt = nextAt.orElse(nowInstant.plusSeconds(3).some)
        )
      else pause

    def pause =
      copy(
        nextAt = none,
        until = none
      )

    def seconds: Option[Int] = until
      .map: u =>
        (u.toSeconds - nowSeconds).toInt
      .filter(0 < _)

    def playing = nextAt.isDefined
    def paused = !playing

    def addLog(event: SyncLog.Event) = copy(log = log.add(event))
    def clearLog = copy(log = SyncLog.empty)

    // subtract estimated source polling lag from transmission delay
    private def pollingLag = Seconds(if isPush then 1 else 6)
    def delayMinusLag = delay.map(_ - pollingLag).filter(_.value > 0)

    override def toString = upstream.toString

  object Sync:

    // either an Int round number,
    // or a string that matches exactly the Round tag
    type OnlyRound = Either[String, Int]
    object OnlyRound:
      def toString(r: OnlyRound) = r.fold(identity, _.toString)
      def parse(s: String) = s.toIntOption.fold(Left(s))(Right(_))

    object url:
      private val lccRegex = """view\.livechesscloud\.com/?#?([0-9a-f\-]+)/(\d+)""".r.unanchored
      extension (url: URL)
        def lcc: Option[Lcc] = url.toString match
          case lccRegex(id, round) => round.toIntOption.map(Lcc(id, _))
          case _ => none
        def looksLikeLcc = url.host.toString.endsWith("livechesscloud.com")
    import url.*

    enum Upstream:
      case Url(url: URL) extends Upstream
      case Urls(urls: List[URL]) extends Upstream
      case Ids(ids: List[GameId]) extends Upstream
      case Users(users: List[UserStr]) extends Upstream
      def asUrl: Option[URL] = this match
        case Url(url) => url.some
        case _ => none
      def isUrl = asUrl.isDefined
      def lcc: Option[Lcc] = asUrl.flatMap(_.lcc)
      def hasLcc = this match
        case Url(url) => url.looksLikeLcc
        case Urls(urls) => urls.exists(_.looksLikeLcc)
        case _ => false
      def hasUnsafeHttp: Option[URL] = this match
        case Url(url) => Option.when(url.scheme == "http")(url)
        case Urls(urls) => urls.find(_.scheme == "http")
        case _ => none
      def roundId: Option[RelayRoundId] = this match
        case Url(url) =>
          url.path.split("/") match
            case Array("", "broadcast", _, _, id) =>
              val cleanId = if id.endsWith(".pgn") then id.dropRight(4) else id
              (cleanId.size == 8).option(RelayRoundId(cleanId))
            case _ => none
        case _ => none
      def isRound = roundId.isDefined
      def roundIds: List[RelayRoundId] = this match
        case url: Url => url.roundId.toList
        case Urls(urls) => urls.map(Url.apply).flatMap(_.roundId)
        case _ => Nil
      def isInternal = this match
        case Ids(_) => true
        case Users(_) => true
        case _ => false

    case class Lcc(id: String, round: Int):
      def pageUrl = URL.parse(s"https://view.livechesscloud.com/#$id/$round")
      def indexUrl = URL.parse(s"http://1.pool.livechesscloud.com/get/$id/round-$round/index.json")
      def gameUrl(g: Int) = URL.parse(s"http://1.pool.livechesscloud.com/get/$id/round-$round/game-$g.json")

  trait AndTour:
    val tour: RelayTour
    def display: RelayRound
    def link: RelayRound
    def fullName = s"${tour.name} • ${display.name}"
    def path: String =
      s"/broadcast/${tour.slug}/${if link.slug == tour.slug then "-" else link.slug}/${link.id}"
    def path(chapterId: StudyChapterId): String = s"$path/$chapterId"

  trait AndGroup:
    def group: Option[RelayGroup.Name]

  trait AndTourAndGroup extends AndTour with AndGroup

  case class WithTour(round: RelayRound, tour: RelayTour) extends AndTour:
    def display = round
    def link = round
    def withStudy(study: Study) = WithTourAndStudy(round, tour, study)

  case class WithTourAndGroup(round: RelayRound, tour: RelayTour, group: Option[RelayGroup.Name])
      extends AndTourAndGroup:
    def display = round
    def link = round
    def withStudy(study: Study) = WithTourAndStudy(round, tour, study)

  case class WithTourAndStudy(relay: RelayRound, tour: RelayTour, study: Study):
    def withTour = WithTour(relay, tour)
    def path = withTour.path
    def fullName = withTour.fullName

  case class WithStudy(relay: RelayRound, study: Study)
