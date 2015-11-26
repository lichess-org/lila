package lila.coach

import chess.{ Color, Status, Role }
import lila.game.PgnMoves
import lila.rating.PerfType
import org.joda.time.DateTime

case class Entry(
    _id: String,
    version: Int,
    userId: String,
    gameId: String,
    color: Color,
    perf: PerfType,
    eco: Option[Ecopening],
    opponent: Opponent,
    moves: List[Move],
    result: Result,
    status: Status,
    finalPhase: Phase,
    ratingDiff: Int,
    analysed: Boolean,
    date: DateTime) {

  def id = _id
}

object Entry {
  val currentVersion = 1
}

case class Move(
  phase: Phase,
  tenths: Int,
  role: Role,
  eval: Option[Int], // before the move was played, relative to player
  mate: Option[Int], // before the move was played, relative to player
  cpl: Option[Int], // eval diff caused by the move, relative to player, mate ~= 10
  opportunism: Boolean)

case class Opponent(rating: Int, strength: RelativeStrength)

sealed abstract class Result(val id: Int, val name: String)
object Result {
  object Win extends Result(1, "Victory")
  object Draw extends Result(2, "Draw")
  object Loss extends Result(3, "Defeat")
  val all = List(Win, Draw, Loss)
}

sealed abstract class Phase(val id: Int, val name: String)
object Phase {
  object Opening extends Phase(1, "Opening")
  object Middle extends Phase(2, "Middlegame")
  object End extends Phase(3, "Endgame")
  val all = List(Opening, Middle, End)
  def of(div: chess.Division, ply: Int): Phase =
    div.middle.fold[Phase](Opening) {
      case m if m > ply => Opening
      case m => div.end.fold[Phase](Middle) {
        case e if e > ply => Middle
        case _            => End
      }
    }
}

sealed abstract class RelativeStrength(val id: Int, val name: String)
object RelativeStrength {
  object MuchWeaker extends RelativeStrength(10, "Much weaker")
  object Weaker extends RelativeStrength(20, "Weaker")
  object Equal extends RelativeStrength(30, "Equal")
  object Stronger extends RelativeStrength(40, "Stronger")
  object MuchStronger extends RelativeStrength(50, "Much stronger")
  val all = List(MuchWeaker, Weaker, Equal, Stronger, MuchStronger)
  def apply(diff: Int) = diff match {
    case d if d < -300 => MuchWeaker
    case d if d < -100 => Weaker
    case d if d > 100  => Stronger
    case d if d > 300  => MuchStronger
    case _             => Equal
  }
}
