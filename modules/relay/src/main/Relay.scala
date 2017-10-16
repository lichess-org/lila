package lila.relay

import org.joda.time.DateTime

import lila.study.{ Study }
import lila.user.User

case class Relay(
    _id: Relay.Id,
    name: String,
    description: String,
    sync: Relay.Sync,
    ownerId: User.ID,
    likes: Study.Likes,
    /* When it's planned to start */
    startsAt: Option[DateTime],
    /* When it actually starts */
    startedAt: Option[DateTime],
    /* at least it *looks* finished... but maybe it's not
     * sync.nextAt is used for actually synchronising
     */
    finished: Boolean,
    createdAt: DateTime
) {

  def id = _id

  def studyId = Study.Id(id.value)

  def slug = {
    val s = lila.common.String slugify name
    if (s.isEmpty) "-" else s
  }

  def finish = copy(
    finished = true,
    sync = sync.pause
  )

  def resume = copy(
    finished = false,
    sync = sync.play
  )

  def ensureStarted = copy(
    startedAt = startedAt orElse DateTime.now.some
  )

  def withSync(f: Relay.Sync => Relay.Sync) = copy(sync = f(sync))

  override def toString = s"""relay #$id "$name" $sync"""
}

object Relay {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(ornicar.scalalib.Random nextString 8)

  case class Sync(
      upstream: Sync.Upstream,
      nextAt: Option[DateTime],
      delay: Option[Int] = None,
      log: SyncLog
  ) {

    def play = copy(nextAt = nextAt orElse DateTime.now.plusSeconds(3).some)
    def pause = copy(nextAt = none)

    def playing = nextAt.isDefined
    def paused = !playing

    override def toString = upstream.toString
  }

  object Sync {
    sealed abstract class Upstream(val key: String, val url: String) {
      override def toString = s"$key $url"
    }
    object Upstream {
      case class DgtOneFile(fileUrl: String) extends Upstream("dgt-one", fileUrl)
      case class DgtManyFiles(dirUrl: String) extends Upstream("dgt-many", dirUrl)
    }
  }

  case class WithStudy(relay: Relay, study: Study)

  case class WithStudyAndLiked(relay: Relay, study: Study, liked: Boolean)

  case class Selection(
      created: List[WithStudyAndLiked],
      started: List[WithStudyAndLiked],
      closed: List[WithStudyAndLiked]
  )
}
