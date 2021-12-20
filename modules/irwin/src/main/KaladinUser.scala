package lila.irwin

import org.joda.time.DateTime

import lila.report.SuspectId
import lila.user.User
import lila.report.Suspect
import lila.user.Holder

case class KaladinUser(
    _id: User.ID,
    priority: Int,
    queuedAt: DateTime,
    queuedBy: User.ID,
    startedAt: Option[DateTime] = None,
    response: Option[KaladinUser.Response] = None
) {

  def suspectId = SuspectId(_id)

  def recentlyQueued = queuedAt isAfter DateTime.now.minusWeeks(1)

  def queueAgain(by: Holder) = copy(
    priority = KaladinUser.priority.MOD,
    queuedAt = DateTime.now,
    queuedBy = by.id,
    response = none
  )
}

object KaladinUser {

  def make(suspect: Suspect, by: Holder) = KaladinUser(
    _id = suspect.id.value,
    priority = priority.MOD,
    queuedAt = DateTime.now,
    queuedBy = by.id
  )

  object priority {
    val MOD  = 100
    val AUTO = 10
  }

  case class Response(at: DateTime, pred: Float)

  case class Dashboard(recent: List[KaladinUser]) {

    def lastSeenAt = recent.view.map(_.response) collectFirst { case Some(response) =>
      response.at
    }

    def seenRecently = lastSeenAt.??(DateTime.now.minusMinutes(30).isBefore)
  }
}
