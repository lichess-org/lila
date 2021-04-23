package lila.relay

import org.joda.time.DateTime

import lila.study.{ Chapter, Study }
import lila.user.User

case class RelayRound(
    _id: RelayRound.Id,
    tourId: RelayTour.Id,
    name: String,
    description: String,
    markup: Option[String] = None,
    credit: Option[String] = None,
    sync: RelayRound.Sync,
    /* When it's planned to start */
    startsAt: Option[DateTime],
    /* When it actually starts */
    startedAt: Option[DateTime],
    /* at least it *looks* finished... but maybe it's not
     * sync.nextAt is used for actually synchronising */
    finished: Boolean,
    createdAt: DateTime
) {

  def id = _id

  def studyId = Study.Id(id.value)

  def slug = {
    val s = lila.common.String slugify name
    if (s.isEmpty) "-" else s
  }

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
      startedAt = startedAt orElse DateTime.now.some
    )

  def hasStarted = startedAt.isDefined

  def shouldGiveUp =
    !hasStarted && (startsAt match {
      case Some(at) => at.isBefore(DateTime.now minusHours 3)
      case None     => createdAt.isBefore(DateTime.now minusDays 1)
    })

  def withSync(f: RelayRound.Sync => RelayRound.Sync) = copy(sync = f(sync))

  def withTour(tour: RelayTour) = RelayRound.WithTour(this, tour)

  override def toString = s"""relay #$id "$name" $sync"""
}

object RelayRound {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)

  case class Sync(
      upstream: Option[Sync.Upstream], // if empty, needs a client to push PGN
      until: Option[DateTime],         // sync until then; resets on move
      nextAt: Option[DateTime],        // when to run next sync
      delay: Option[Int],              // override time between two sync (rare)
      log: SyncLog
  ) {

    def hasUpstream = upstream.isDefined

    def renew =
      if (hasUpstream) copy(until = DateTime.now.plusHours(1).some)
      else pause

    def ongoing = until ?? DateTime.now.isBefore

    def play =
      if (hasUpstream) renew.copy(nextAt = nextAt orElse DateTime.now.plusSeconds(3).some)
      else pause

    def pause =
      copy(
        nextAt = none,
        until = none
      )

    def seconds: Option[Int] =
      until map { u =>
        (u.getSeconds - nowSeconds).toInt
      } filter (0 <)

    def playing = nextAt.isDefined
    def paused  = !playing

    def addLog(event: SyncLog.Event) = copy(log = log add event)
    def clearLog                     = copy(log = SyncLog.empty)

    override def toString = upstream.toString
  }

  object Sync {
    sealed trait Upstream {
      def asUrl: Option[UpstreamUrl] = this match {
        case url: UpstreamUrl => url.some
        case _                => none
      }
      def local = asUrl.fold(true)(_.isLocal)
    }
    case class UpstreamUrl(url: String) extends Upstream {
      def isLocal = url.contains("://127.0.0.1") || url.contains("://localhost")
      def withRound =
        url.split(" ", 2) match {
          case Array(u, round) => UpstreamUrl.WithRound(u, round.toIntOption)
          case _               => UpstreamUrl.WithRound(url, none)
        }
    }
    object UpstreamUrl {
      case class WithRound(url: String, round: Option[Int])
      val LccRegex = """.*view\.livechesscloud\.com/#?([0-9a-f\-]+)""".r
    }
    case class UpstreamIds(ids: List[lila.game.Game.ID]) extends Upstream
  }

  case class WithTour(relay: RelayRound, tour: RelayTour) {
    def fullName = s"${tour.name} • ${relay.name}"

    def withStudy(study: Study) = WithTourAndStudy(relay, tour, study)

    def path: String                        = s"/broadcast/${tour.slug}/${relay.slug}/${relay.id}"
    def path(chapterId: Chapter.Id): String = s"$path/$chapterId"
  }

  case class WithTourAndStudy(relay: RelayRound, tour: RelayTour, study: Study) {
    def path     = WithTour(relay, tour).path
    def fullName = WithTour(relay, tour).fullName
  }

  case class Fresh(created: Seq[WithTour], started: Seq[WithTour])
}
