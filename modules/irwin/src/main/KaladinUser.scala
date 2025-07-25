package lila.irwin

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.perf.PerfId
import lila.core.report.SuspectId
import lila.rating.PerfType
import lila.report.Suspect

case class KaladinUser(
    @Key("_id") id: UserId,
    priority: Int,
    queuedAt: Instant,
    queuedBy: KaladinUser.Requester,
    startedAt: Option[Instant] = None,
    response: Option[KaladinUser.Response] = None
):
  def suspectId = SuspectId(id)

  def recentlyQueued = queuedAt.isAfter(nowInstant.minusWeeks(1))

  def queueAgain(by: KaladinUser.Requester): Option[KaladinUser] =
    if startedAt.isEmpty && by.priority > priority then
      copy(
        priority = by.priority,
        queuedBy = by
      ).some
    else if by.isMod || !recentlyQueued then
      copy(
        priority = by.priority,
        queuedAt = nowInstant,
        queuedBy = by,
        startedAt = none,
        response = none
      ).some
    else none

object KaladinUser:

  def make(suspect: Suspect, by: Requester) = KaladinUser(
    id = suspect.id.value,
    priority = by.priority,
    queuedAt = nowInstant,
    queuedBy = by
  )

  sealed abstract class Requester(val priority: Int):
    def name = Requester.this.toString
    def isMod = false
  object Requester:
    case class Mod(id: UserId) extends Requester(100):
      override def name = id.value
      override def isMod = true
    case object TopOnline extends Requester(10)
    case object TournamentLeader extends Requester(20)
    case object Report extends Requester(30)

  case class Response(at: Instant, pred: Option[Pred], err: Option[String])
  // Pred, short for Predication, activation, float between 0 and 1,
  // the higher the more likely the user is cheating
  case class Pred(activation: Float, insights: List[String], tc: Int):
    def percent = (activation * 100).toInt
    def perf = PerfType(PerfId(tc))

    def note: String = {
      s"Kaladin activation: $percent in ${perf.fold("?")(_.key)}, because:" :: insights
    }.mkString(", ")

  case class Dashboard(recent: List[KaladinUser]):

    def lastSeenAt = recent.view.map(_.response).collectFirst { case Some(response) =>
      response.at
    }

    def seenRecently = lastSeenAt.so(nowInstant.minusMinutes(30).isBefore)
