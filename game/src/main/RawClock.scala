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

private[game] object RawClock {

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

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private def defaults = Json.obj("t" -> none[Double])

  lazy val tube = Tube(
    reader = (__.json update merge(defaults)) andThen Json.reads[RawClock],
    writer = Json.writes[RawClock]
  )
}
