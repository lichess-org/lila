package lila.relay

import ornicar.scalalib.ThreadLocalRandom

import lila.study.Study

case class RelayRound(
    _id: RelayRoundId,
    tourId: RelayTour.Id,
    name: RelayRoundName,
    caption: Option[RelayRound.Caption],
    sync: RelayRound.Sync,
    /* When it's planned to start */
    startsAt: Option[Instant],
    /* When it actually starts */
    startedAt: Option[Instant],
    /* at least it *looks* finished... but maybe it's not
     * sync.nextAt is used for actually synchronising */
    finished: Boolean,
    createdAt: Instant
):

  inline def id = _id

  inline def studyId = id into StudyId

  lazy val slug =
    val s = lila.common.String slugify name.value
    if (s.isEmpty) "-" else s

  def finish =
    copy(
      finished = true,
      sync = sync.pause
    )

  def resume =
    copy(
      finished = false,
      sync = sync.play
    )

  def ensureStarted =
    copy(
      startedAt = startedAt orElse nowInstant.some
    )

  def hasStarted        = startedAt.isDefined
  def hasStartedEarly   = hasStarted && startsAt.exists(_ isAfter nowInstant)
  def shouldHaveStarted = hasStarted || startsAt.exists(_ isBefore nowInstant)

  def shouldGiveUp =
    !hasStarted && (startsAt match {
      case Some(at) => at.isBefore(nowInstant minusHours 3)
      case None     => createdAt.isBefore(nowInstant minusDays 1)
    })

  def withSync(f: RelayRound.Sync => RelayRound.Sync) = copy(sync = f(sync))

  def withTour(tour: RelayTour) = RelayRound.WithTour(this, tour)

  override def toString = s"""relay #$id "$name" $sync"""

object RelayRound:

  def makeId = RelayRoundId(ThreadLocalRandom nextString 8)

  opaque type Caption = String
  object Caption extends OpaqueString[Caption]

  case class Sync(
      upstream: Option[Sync.Upstream], // if empty, needs a client to push PGN
      until: Option[Instant],          // sync until then; resets on move
      nextAt: Option[Instant],         // when to run next sync
      delay: Option[Int],              // override time between two sync (rare)
      log: SyncLog
  ):

    def hasUpstream = upstream.isDefined

    def renew =
      if (hasUpstream) copy(until = nowInstant.plusHours(1).some)
      else pause

    def ongoing = until ?? nowInstant.isBefore

    def play =
      if (hasUpstream) renew.copy(nextAt = nextAt orElse nowInstant.plusSeconds(3).some)
      else pause

    def pause =
      copy(
        nextAt = none,
        until = none
      )

    def seconds: Option[Int] =
      until map { u =>
        (u.toSeconds - nowSeconds).toInt
      } filter (0 <)

    def playing = nextAt.isDefined
    def paused  = !playing

    def addLog(event: SyncLog.Event) = copy(log = log add event)
    def clearLog                     = copy(log = SyncLog.empty)

    override def toString = upstream.toString

  object Sync:
    sealed trait Upstream:
      def asUrl: Option[UpstreamUrl] = this match
        case url: UpstreamUrl => url.some
        case _                => none
      def local = asUrl.fold(true)(_.isLocal)
    case class UpstreamUrl(url: String) extends Upstream:
      def isLocal = url.contains("://127.0.0.1") || url.contains("://[::1]") || url.contains("://localhost")
      def withRound =
        url.split(" ", 2) match
          case Array(u, round) => UpstreamUrl.WithRound(u, round.toIntOption)
          case _               => UpstreamUrl.WithRound(url, none)
    object UpstreamUrl:
      case class WithRound(url: String, round: Option[Int])
      val LccRegex = """.*view\.livechesscloud\.com/#?([0-9a-f\-]+)""".r
    case class UpstreamIds(ids: List[GameId]) extends Upstream

  trait AndTour:
    val round: RelayRound
    val tour: RelayTour
    def fullName = s"${tour.name} • ${round.name}"
    def path: String =
      s"/broadcast/${tour.slug}/${if (round.slug == tour.slug) "-" else round.slug}/${round.id}"
    def path(chapterId: StudyChapterId): String = s"$path/$chapterId"

  case class WithTour(round: RelayRound, tour: RelayTour) extends AndTour:
    def withStudy(study: Study) = WithTourAndStudy(round, tour, study)

  case class WithTourAndStudy(relay: RelayRound, tour: RelayTour, study: Study):
    def path     = WithTour(relay, tour).path
    def fullName = WithTour(relay, tour).fullName
