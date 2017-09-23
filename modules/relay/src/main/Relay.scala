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
    createdAt: DateTime,
    startsAt: DateTime,
    closedAt: Option[DateTime]
) {

  def id = _id

  def studyId = Study.Id(id.value)

  def slug = lila.common.String slugify name

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
