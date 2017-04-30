package lila.irwin

import org.joda.time.DateTime

import lila.user.User

case class IrwinRequest(
    _id: User.ID,
    origin: IrwinRequest.Origin,
    priority: DateTime, // older = more prioritary; affected by origin
    createdAt: DateTime,
    startedAt: Option[DateTime]
) {

  def id = _id
}

object IrwinRequest {

  sealed trait Origin

  object Origin {
    case class Moderator(id: User.ID) extends Origin
    case class UserReport(reportId: String) extends Origin
    case class AutoReport(reportId: String) extends Origin
    case class Tournament(tourId: String) extends Origin
  }

  def make(userId: User.ID, origin: Origin) = IrwinRequest(
    _id = userId,
    origin = origin,
    priority = DateTime.now plusHours originPriorityHours(origin),
    createdAt = DateTime.now,
    startedAt = none
  )

  private def originPriorityHours(origin: Origin) = origin match {
    case Origin.Moderator(_) => 100
    case Origin.Tournament(_) => 10
    case Origin.UserReport(_) => 0
    case Origin.AutoReport(_) => 0
  }
}
