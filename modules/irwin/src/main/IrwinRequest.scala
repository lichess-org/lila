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

  def isInProgress = startedAt.isDefined
}

object IrwinRequest {

  sealed trait Origin {
    def key = toString.toLowerCase
  }

  object Origin {
    case object Moderator extends Origin
    case object Report extends Origin
    case object Tournament extends Origin
    case object Leaderboard extends Origin
  }

  def make(userId: User.ID, origin: Origin) = IrwinRequest(
    _id = userId,
    origin = origin,
    priority = DateTime.now minusHours originPriorityDays(origin),
    createdAt = DateTime.now,
    startedAt = none
  )

  private def originPriorityDays(origin: Origin) = origin match {
    case Origin.Moderator => 100
    case Origin.Report => 20
    case Origin.Tournament => 1
    case Origin.Leaderboard => 0
  }
}
