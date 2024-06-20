package lila.relay

import scalalib.ThreadLocalRandom
import reactivemongo.api.bson.Macros.Annotations.Key

import scalalib.model.Seconds
import lila.study.Study

case class RelayRound(
    /* Same as the Study id it refers to */
    @Key("_id") id: RelayRoundId,
    tourId: RelayTourId,
    name: RelayRound.Name,
    caption: Option[RelayRound.Caption],
    sync: RelayRound.Sync,
    /* When it's planned to start */
    startsAt: Option[Instant],
    /* When it actually starts */
    startedAt: Option[Instant],
    /* at least it *looks* finished... but maybe it's not
     * sync.nextAt is used for actually synchronising */
    finished: Boolean,
    createdAt: Instant,
    crowd: Option[Int]
):
  inline def studyId = id.into(StudyId)

  lazy val slug =
    val s = scalalib.StringOps.slug(name.value)
    if s.isEmpty then "-" else s

  def finish =
    copy(
      finished = true,
      sync = sync.pause
    )

  def resume(official: Boolean) =
    copy(
      finished = false,
      sync = sync.play(official)
    )

  def ensureStarted     = copy(startedAt = startedAt.orElse(nowInstant.some))
  def hasStarted        = startedAt.isDefined
  def hasStartedEarly   = startedAt.exists(at => startsAt.exists(_.isAfter(at)))
  def shouldHaveStarted = hasStarted || startsAt.exists(_.isBefore(nowInstant))

  def shouldGiveUp =
    !hasStarted && startsAt.match
      case Some(at) => at.isBefore(nowInstant.minusHours(3))
      case None     => createdAt.isBefore(nowInstant.minusDays(1))

  def stateHash = (hasStarted, finished)

  def withSync(f: RelayRound.Sync => RelayRound.Sync) = copy(sync = f(sync))

  def withTour(tour: RelayTour) = RelayRound.WithTour(this, tour)

  override def toString = s"""relay #$id "$name" $sync"""

object RelayRound:

  def makeId = RelayRoundId(ThreadLocalRandom.nextString(8))

  opaque type Name = String
  object Name extends OpaqueString[Name]

  opaque type Caption = String
  object Caption extends OpaqueString[Caption]

  case class Sync(
      upstream: Option[Sync.Upstream], // if empty, needs a client to push PGN
      until: Option[Instant],          // sync until then; resets on move
      nextAt: Option[Instant],         // when to run next sync
      period: Option[Seconds],         // override time between two sync (rare)
      delay: Option[Seconds],          // add delay between the source and the study
      onlyRound: Option[Int],          // only keep games with [Round "x"]
      slices: Option[List[RelayGame.Slice]] = none,
      log: SyncLog
  ):
    def hasUpstream = upstream.isDefined

    def renew(official: Boolean) =
      if hasUpstream then copy(until = nowInstant.plusHours(if official then 3 else 1).some)
      else pause

    def ongoing = until.so(nowInstant.isBefore)

    def play(official: Boolean) =
      if hasUpstream then renew(official).copy(nextAt = nextAt.orElse(nowInstant.plusSeconds(3).some))
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
    def paused  = !playing

    def addLog(event: SyncLog.Event) = copy(log = log.add(event))
    def clearLog                     = copy(log = SyncLog.empty)

    def nonEmptyDelay = delay.filter(_.value > 0)
    def hasDelay      = nonEmptyDelay.isDefined

    override def toString = upstream.toString

  object Sync:
    sealed trait Upstream:
      def isLcc = false
    sealed trait FetchableUpstream extends Upstream:
      def fetchUrl: String
      def formUrl: String
    case class UpstreamUrl(url: String) extends FetchableUpstream:
      def fetchUrl = url
      def formUrl  = url
    case class UpstreamUrls(urls: List[FetchableUpstream]) extends Upstream
    case class UpstreamIds(ids: List[GameId])              extends Upstream
    case class UpstreamLcc(lcc: String, round: Int) extends FetchableUpstream:
      override def isLcc = true
      def id             = lcc
      def fetchUrl       = s"http://1.pool.livechesscloud.com/get/$id/round-$round/index.json"
      def viewUrl        = s"https://view.livechesscloud.com/#$id"
      def formUrl        = s"$viewUrl $round"
    object UpstreamLcc:
      private val idRegex = """.*view\.livechesscloud\.com/?#?([0-9a-f\-]+)""".r
      def findId(url: UpstreamUrl): Option[String] = url.url match
        case idRegex(id) => id.some
        case _           => none
      def find(url: String): Option[UpstreamLcc] = url.split(' ').map(_.trim).filter(_.nonEmpty) match
        case Array(idRegex(id), round) => round.toIntOption.map(UpstreamLcc(id, _))
        case _                         => none

  trait AndTour:
    val tour: RelayTour
    def display: RelayRound
    def link: RelayRound
    def fullName = s"${tour.name} • ${display.name}"
    def path: String =
      s"/broadcast/${tour.slug}/${if link.slug == tour.slug then "-" else link.slug}/${link.id}"
    def path(chapterId: StudyChapterId): String = s"$path/$chapterId"
    def crowd                                   = display.crowd.orElse(link.crowd)

  trait AndGroup:
    def group: Option[RelayGroup.Name]

  trait AndTourAndGroup extends AndTour with AndGroup

  case class WithTour(round: RelayRound, tour: RelayTour) extends AndTour:
    def display                 = round
    def link                    = round
    def withStudy(study: Study) = WithTourAndStudy(round, tour, study)

  case class WithTourAndGroup(round: RelayRound, tour: RelayTour, group: Option[RelayGroup.Name])
      extends AndTourAndGroup:
    def display                 = round
    def link                    = round
    def withStudy(study: Study) = WithTourAndStudy(round, tour, study)

  case class WithTourAndStudy(relay: RelayRound, tour: RelayTour, study: Study):
    def withTour = WithTour(relay, tour)
    def path     = withTour.path
    def fullName = withTour.fullName

  case class WithStudy(relay: RelayRound, study: Study)
