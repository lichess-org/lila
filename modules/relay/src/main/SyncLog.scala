package lila.relay

import org.joda.time.DateTime

case class SyncLog(events: List[SyncLog.Event]) extends AnyVal {

  def +(e: SyncLog.Event) = SyncLog {
    e :: events.take(SyncLog.historySize - 1)
  }

  def isOk = events.headOption ?? (_.ok)
}

object SyncLog {

  val historySize = 5

  case class Event(
      ok: Boolean,
      msg: String,
      at: DateTime
  )
}
