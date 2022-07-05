package lila.insight

import chess.{ Centis, Clock, Color, Role }
import scala.concurrent.duration.FiniteDuration

import lila.analyse.{ AccuracyPercent, WinPercent }

case class MeanRating(value: Int) extends AnyVal

case class InsightMove(
    phase: Phase,
    tenths: Int, // tenths of seconds spent thinking
    clockPercent: ClockPercent,
    role: Role,
    eval: Option[Int],              // before the move was played, relative to player
    cpl: Option[Int],               // eval diff caused by the move, relative to player, mate ~= 10
    winPercent: Option[WinPercent], // before the move was played, relative to player
    accuracyPercent: Option[AccuracyPercent],
    material: Int, // material imbalance, relative to player
    awareness: Option[Boolean],
    luck: Option[Boolean],
    blur: Boolean,
    timeCv: Option[Float] // time coefficient variation
)

// time remaining on clock, accounting for increment via estimation
case class ClockPercent private (value: Double) extends AnyVal with Percent

object ClockPercent {
  def apply(clock: Clock.Config, timeLeft: Centis) = new ClockPercent(
    (100 * timeLeft.centis.toDouble / clock.estimateTotalTime.centis) atLeast 0 atMost 100
  )
  def fromPercent(p: Double) = ClockPercent(p)
  def fromPercent(p: Int)    = ClockPercent(p.toDouble)
}

sealed abstract class Termination(val id: Int, val name: String)
object Termination {
  case object ClockFlag   extends Termination(1, "Clock flag")
  case object Disconnect  extends Termination(2, "Disconnect")
  case object Resignation extends Termination(3, "Resignation")
  case object Draw        extends Termination(4, "Draw")
  case object Stalemate   extends Termination(5, "Stalemate")
  case object Checkmate   extends Termination(6, "Checkmate")

  val all = List[Termination](ClockFlag, Disconnect, Resignation, Draw, Stalemate, Checkmate)
  val byId = all map { p =>
    (p.id, p)
  } toMap

  import chess.{ Status => S }

  def fromStatus(s: chess.Status) =
    s match {
      case S.Timeout             => Disconnect
      case S.Outoftime           => ClockFlag
      case S.Resign              => Resignation
      case S.Draw                => Draw
      case S.Stalemate           => Stalemate
      case S.Mate | S.VariantEnd => Checkmate
      case S.Cheat               => Resignation
      case S.Created | S.Started | S.Aborted | S.NoStart | S.UnknownFinish =>
        logger.error(s"Unfinished game in the insight indexer: $s")
        Resignation
    }
}

sealed abstract class Result(val id: Int, val name: String)
object Result {
  case object Win  extends Result(1, "Victory")
  case object Draw extends Result(2, "Draw")
  case object Loss extends Result(3, "Defeat")
  val all = List[Result](Win, Draw, Loss)
  val byId = all map { p =>
    (p.id, p)
  } toMap
  val idList = all.map(_.id)
}

sealed abstract class Phase(val id: Int, val name: String)
object Phase {
  case object Opening extends Phase(1, "Opening")
  case object Middle  extends Phase(2, "Middlegame")
  case object End     extends Phase(3, "Endgame")
  val all = List[Phase](Opening, Middle, End)
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def of(div: chess.Division, ply: Int): Phase =
    div.middle.fold[Phase](Opening) {
      case m if m > ply => Opening
      case _ =>
        div.end.fold[Phase](Middle) {
          case e if e > ply => Middle
          case _            => End
        }
    }
}

sealed abstract class Castling(val id: Int, val name: String)
object Castling {
  object Kingside  extends Castling(1, "Kingside castling")
  object Queenside extends Castling(2, "Queenside castling")
  object None      extends Castling(3, "No castling")
  val all = List(Kingside, Queenside, None)
  val byId = all map { p =>
    (p.id, p)
  } toMap
  def fromMoves(moves: Iterable[String]) =
    moves.find(_ startsWith "O") match {
      case Some("O-O")   => Kingside
      case Some("O-O-O") => Queenside
      case _             => None
    }
}

sealed abstract class QueenTrade(val id: Boolean, val name: String)
object QueenTrade {
  object Yes extends QueenTrade(true, "Queen trade")
  object No  extends QueenTrade(false, "No queen trade")
  val all                           = List(Yes, No)
  def apply(v: Boolean): QueenTrade = if (v) Yes else No
}

sealed abstract class Blur(val id: Boolean, val name: String)
object Blur {
  object Yes extends Blur(true, "Blur")
  object No  extends Blur(false, "No blur")
  val all                     = List(Yes, No)
  def apply(v: Boolean): Blur = if (v) Yes else No
}
