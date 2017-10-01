package lila.relay

import org.joda.time.DateTime

case class SyncLog(events: Vector[SyncLog.Event]) extends AnyVal {

  def isOk = events.lastOption ?? (_.isOk)

  def alwaysFails = events.size == SyncLog.historySize && events.forall(_.isKo)

  def updatedAt = events.lastOption.map(_.at)
}

object SyncLog {

  val historySize = 5

  case class Event(
      error: Option[String],
      at: DateTime
  ) {
    def isOk = error.isEmpty
    def isKo = error.nonEmpty
  }
}
