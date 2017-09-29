package lila.relay

import org.joda.time.DateTime

case class SyncLog(events: List[SyncLog.Event]) extends AnyVal {

  def +(e: SyncLog.Event) = SyncLog {
    e :: events.take(4)
  }

  def isOk = events.headOption ?? (_.ok)
}

object SyncLog {

  case class Event(
      ok: Boolean,
      msg: String,
      at: DateTime
  )
}
