package lila.insight

import chess.{ Color, Role }
import lila.game.{ PgnMoves, Game }
import lila.rating.PerfType
import org.joda.time.DateTime

case class Entry(
    _id: String, // gameId + w/b
    userId: String,
    color: Color,
    perf: PerfType,
    eco: Option[Ecopening],
    opponent: Opponent,
    moves: List[Move],
    result: Result,
    termination: Termination,
    finalPhase: Phase,
    ratingDiff: Int,
    analysed: Boolean,
    date: DateTime) {

  def id = _id

  def gameId = id take Game.gameIdSize
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

sealed abstract class Termination(val id: Int, val name: String)
object Termination {
  case object ClockFlag extends Termination(1, "Clock flag")
  case object RageQuit extends Termination(2, "Rage quit")
  case object Resignation extends Termination(3, "Resignation")
  case object Draw extends Termination(4, "Draw")
  case object Stalemate extends Termination(5, "Stalemate")
  case object Checkmate extends Termination(6, "Checkmate")

  val all = List(ClockFlag, RageQuit, Resignation, Draw, Stalemate, Checkmate)
  val byId = all map { p => (p.id, p) } toMap

  import chess.{ Status => S }

  def fromStatus(s: chess.Status) = s match {
    case S.Timeout             => RageQuit
    case S.Outoftime           => ClockFlag
    case S.Resign              => Resignation
    case S.Draw                => Draw
    case S.Stalemate           => Stalemate
    case S.Mate | S.VariantEnd => Checkmate
    case S.Cheat               => Resignation
    case S.Created | S.Started | S.Aborted | S.NoStart =>
      logwarn(s"[insight] Unfinished game in the insight indexer")
      Resignation
  }
}

sealed abstract class Result(val id: Int, val name: String)
object Result {
  case object Win extends Result(1, "Victory")
  case object Draw extends Result(2, "Draw")
  case object Loss extends Result(3, "Defeat")
  val all = List(Win, Draw, Loss)
  val byId = all map { p => (p.id, p) } toMap
  val idList = all.map(_.id)
}

sealed abstract class Phase(val id: Int, val name: String)
object Phase {
  case object Opening extends Phase(1, "Opening")
  case object Middle extends Phase(2, "Middlegame")
  case object End extends Phase(3, "Endgame")
  val all = List(Opening, Middle, End)
  val byId = all map { p => (p.id, p) } toMap
  def of(div: chess.Division, ply: Int): Phase =
    div.middle.fold[Phase](Opening) {
      case m if m > ply => Opening
      case m => div.end.fold[Phase](Middle) {
        case e if e > ply => Middle
        case _            => End
      }
    }
}

// sealed abstract class Castling(val id: Int, val name: String)
// object Castling {
//   object Kingside extends Phase(1, "Kingside castling")
//   object Queenside extends Phase(2, "Queenside castling")
//   object None extends Phase(3, "No castling")
//   val all = List(Kingside, Queenside, None)
//   val byId = all map { p => (p.id, p) } toMap
// }

sealed abstract class RelativeStrength(val id: Int, val name: String)
object RelativeStrength {
  case object MuchWeaker extends RelativeStrength(10, "Much weaker")
  case object Weaker extends RelativeStrength(20, "Weaker")
  case object Equal extends RelativeStrength(30, "Equal")
  case object Stronger extends RelativeStrength(40, "Stronger")
  case object MuchStronger extends RelativeStrength(50, "Much stronger")
  val all = List(MuchWeaker, Weaker, Equal, Stronger, MuchStronger)
  val byId = all map { p => (p.id, p) } toMap
  def apply(diff: Int) = diff match {
    case d if d < -300 => MuchWeaker
    case d if d < -100 => Weaker
    case d if d > 100  => Stronger
    case d if d > 300  => MuchStronger
    case _             => Equal
  }
}
