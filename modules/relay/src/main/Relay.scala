package lila.relay

import org.joda.time.DateTime

import lila.study.Study
import lila.user.User

case class Relay(
    _id: Relay.Id,
    name: String,
    description: String,
    markup: Option[String] = None,
    credit: Option[String] = None,
    sync: Relay.Sync,
    ownerId: User.ID,
    likes: Study.Likes,
    /* When it's planned to start */
    startsAt: Option[DateTime],
    /* When it actually starts */
    startedAt: Option[DateTime],
    /* at least it *looks* finished... but maybe it's not
     * sync.nextAt is used for actually synchronising */
    finished: Boolean,
    official: Boolean,
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

  def withSync(f: Relay.Sync => Relay.Sync) = copy(sync = f(sync))

  override def toString = s"""relay #$id "$name" $sync"""
}

object Relay {

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
    case class Upstream(url: String) extends AnyVal {
      def isLocal = url.contains("://127.0.0.1") || url.contains("://localhost")
      def withRound =
        url.split(" ", 2) match {
          case Array(u, round) => UpstreamWithRound(u, round.toIntOption)
          case _               => UpstreamWithRound(url, none)
        }
    }
    case class UpstreamWithRound(url: String, round: Option[Int])
    val LccRegex = """.*view\.livechesscloud\.com/#?([0-9a-f\-]+)""".r
  }

  case class WithStudy(relay: Relay, study: Study)

  case class WithStudyAndLiked(relay: Relay, study: Study, liked: Boolean)

  case class Fresh(
      created: Seq[WithStudyAndLiked],
      started: Seq[WithStudyAndLiked]
  )
}
