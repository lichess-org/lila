package lila.relay

case class SyncLog(events: Vector[SyncLog.Event]) extends AnyVal:

  def isOk = events.lastOption.exists(_.isOk)

  def alwaysFails = events.sizeIs >= SyncLog.historySize && events.forall(_.isKo)

  def justTimedOut = events.lastOption.exists(_.isTimeout)

  def updatedAt = events.lastOption.map(_.at)

  def lastErrors: List[String] = events.reverse.takeWhile(_.isKo).flatMap(_.error).toList

  def add(event: SyncLog.Event) =
    copy(
      events = {
        if events.sizeIs > SyncLog.historySize then events.drop(1)
        else events
      } :+ event
    )

object SyncLog:

  val historySize = 5

  val empty = SyncLog(Vector.empty)

  case class Event(
      moves: Int,
      error: Option[String],
      at: Instant
  ):
    export error.{ isEmpty as isOk, nonEmpty as isKo }
    def hasMoves = moves > 0
    def isTimeout = error.has(SyncResult.Timeout.getMessage)

  def event(moves: Int, e: Option[Exception]) =
    Event(
      moves = moves,
      error = e.map {
        case _: java.util.concurrent.TimeoutException => "Request timeout"
        case e: Exception => e.getMessage.take(100)
      },
      at = nowInstant
    )
