package lila.game

import chess.{ Game ⇒ _, _ }
import scala.math.round

private[game] case class RawClock(
    c: Boolean,
    i: Int,
    l: Int,
    w: Float,
    b: Float,
    t: Option[Double] = None) {

  def decode: Clock = t.fold(
    PausedClock(
      color = Color(c),
      increment = i,
      limit = l,
      whiteTime = w,
      blackTime = b): Clock) { timer ⇒
      RunningClock(
        color = Color(c),
        increment = i,
        limit = l,
        whiteTime = w,
        blackTime = b,
        timer = timer)
    }
}

private[game] object RawClocks {

  def encode(clock: Clock): RawClock = {
    import clock._
    RawClock(
      c = color.white,
      i = increment,
      l = limit,
      w = whiteTime,
      b = blackTime,
      t = timerOption
    )
  }

  import lila.db.JsonTube
  import JsonTube.Helpers._
  import play.api.libs.json._

  private val defaults = Json.obj("t" -> none[Double])

  val json = JsonTube(
    reads = (__.json update merge(defaults)) andThen Json.reads[RawClock],
    writes = Json.writes[RawClock]
  )
}
