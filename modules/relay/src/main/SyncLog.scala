package lila.relay

import org.joda.time.DateTime

case class SyncLog(events: Vector[SyncLog.Event]) extends AnyVal {

  def isOk = events.lastOption.exists(_.isOk)

  def alwaysFails = events.sizeIs == SyncLog.historySize && events.forall(_.isKo)

  def justTimedOut = events.lastOption.exists(_.isTimeout)

  def updatedAt = events.lastOption.map(_.at)

  def add(event: SyncLog.Event) =
    copy(
      events = events.take(SyncLog.historySize - 1) :+ event
    )
}

object SyncLog {

  val historySize = 5

  val empty = SyncLog(Vector.empty)

  case class Event(
      moves: Int,
      error: Option[String],
      at: DateTime
  ) {
    def isOk      = error.isEmpty
    def isKo      = error.nonEmpty
    def isTimeout = error has SyncResult.Timeout.getMessage
  }

  def event(moves: Int, e: Option[Exception]) =
    Event(
      moves = moves,
      error = e map {
        case _: java.util.concurrent.TimeoutException => "Request timeout"
        case e: Exception                             => e.getMessage take 100
      },
      at = DateTime.now
    )
}
