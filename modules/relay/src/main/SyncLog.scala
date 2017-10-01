package lila.relay

import org.joda.time.DateTime

case class SyncLog(events: Vector[SyncLog.Event]) extends AnyVal {

  def isOk = events.lastOption ?? (_.isOk)
}

object SyncLog {

  val historySize = 5

  case class Event(
      error: Option[String],
      at: DateTime
  ) {
    def isOk = error.isEmpty
  }
}
