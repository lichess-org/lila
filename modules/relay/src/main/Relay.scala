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
    startsAt: Option[DateTime],
    createdAt: DateTime
) {

  def id = _id

  def studyId = Study.Id(id.value)

  def slug = {
    val s = lila.common.String slugify name
    if (s.isEmpty) "-" else s
  }

  def setSync(v: Boolean) = copy(sync = sync set v)

  override def toString = s"id:$id sync:$sync"
}

object Relay {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(ornicar.scalalib.Random nextString 8)

  case class Sync(upstream: Sync.Upstream, until: Option[DateTime], log: SyncLog) {

    def seconds: Option[Int] = until map { until =>
      (until.getSeconds - nowSeconds).toInt
    } filter (0<)

    def set(v: Boolean) = copy(
      until = v option DateTime.now.plusHours(3),
      log = SyncLog(Vector.empty)
    )
  }

  object Sync {
    sealed abstract class Upstream(val key: String, val url: String)
    object Upstream {
      case class DgtOneFile(fileUrl: String) extends Upstream("dgt-one", fileUrl)
      case class DgtManyFiles(dirUrl: String) extends Upstream("dgt-many", dirUrl)
    }
  }

  case class WithStudy(relay: Relay, study: Study)

  case class Selection(
      created: List[WithStudy],
      started: List[WithStudy],
      closed: List[WithStudy]
  )
}
