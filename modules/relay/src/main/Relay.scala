package lila.relay

import org.joda.time.DateTime

import lila.study.{ Study }
import lila.user.User

case class Relay(
    _id: Relay.Id,
    name: String,
    description: String,
    pgnUrl: String,
    ownerId: User.ID,
    startsAt: Option[DateTime],
    syncUntil: Option[DateTime],
    syncLog: SyncLog,
    createdAt: DateTime
) {

  def id = _id

  def studyId = Study.Id(id.value)

  def slug = lila.common.String slugify name

  def syncSeconds: Option[Int] = syncUntil map { until =>
    (until.getSeconds - nowSeconds).toInt
  } filter (0<)

  override def toString = s"id:$id pgnUrl:$pgnUrl"
}

object Relay {

  case class Id(value: String) extends AnyVal with StringValue

  def makeId = Id(ornicar.scalalib.Random nextString 8)

  case class WithStudy(relay: Relay, study: Study)

  case class Selection(
      created: List[WithStudy],
      started: List[WithStudy],
      closed: List[WithStudy]
  )
}
