package lila.insight

import scalalib.model.Percent
import chess.format.pgn.SanStr
import chess.{ Centis, Clock, Ply, Role }
import chess.eval.WinPercent

import lila.analyse.AccuracyPercent
import lila.common.{ LilaOpeningFamily, SimpleOpening }

case class InsightUser(
    count: Int, // nb insight entries
    families: List[LilaOpeningFamily],
    openings: List[SimpleOpening]
):
  def isEmpty = count == 0

opaque type MeanRating = Int
object MeanRating extends OpaqueInt[MeanRating]

case class InsightMove(
    phase: Phase,
    tenths: Option[Int], // tenths of seconds spent thinking
    clockPercent: Option[ClockPercent],
    role: Role,
    eval: Option[Int], // before the move was played, relative to player
    cpl: Option[Int], // eval diff caused by the move, relative to player, mate ~= 10
    winPercent: Option[WinPercent], // before the move was played, relative to player
    accuracyPercent: Option[AccuracyPercent],
    material: Int, // material imbalance, relative to player
    awareness: Option[Boolean],
    luck: Option[Boolean],
    blur: Boolean,
    timeCv: Option[Float] // time coefficient variation
)

// time remaining on clock, accounting for increment via estimation
opaque type ClockPercent = Double
object ClockPercent extends OpaqueDouble[ClockPercent]:

  given Percent[ClockPercent] = Percent.of(ClockPercent)

  extension (a: ClockPercent) def toInt = Percent.toInt(a)

  def apply(clock: Clock.Config, timeLeft: Centis): ClockPercent = ClockPercent(
    (100 * timeLeft.centis.toDouble / clock.estimateTotalTime.centis).atLeast(0).atMost(100)
  )
  inline def fromPercent(p: Double) = ClockPercent(p)
  inline def fromPercent(p: Int) = ClockPercent(p.toDouble)

enum Termination(val id: Int, val name: String):

  case ClockFlag extends Termination(1, "Clock flag")
  case Disconnect extends Termination(2, "Disconnect")
  case Resignation extends Termination(3, "Resignation")
  case Draw extends Termination(4, "Draw")
  case Stalemate extends Termination(5, "Stalemate")
  case Checkmate extends Termination(6, "Checkmate")

object Termination:

  val byId = values.mapBy(_.id)

  import chess.Status as S

  def fromStatus(s: chess.Status) =
    s match
      case S.Timeout => Disconnect
      case S.Outoftime => ClockFlag
      case S.Resign => Resignation
      case S.Draw | S.InsufficientMaterialClaim => Draw
      case S.Stalemate => Stalemate
      case S.Mate | S.VariantEnd => Checkmate
      case S.Cheat => Resignation
      case S.Created | S.Started | S.Aborted | S.NoStart | S.UnknownFinish =>
        logger.error(s"Unfinished game in the insight indexer: $s")
        Resignation

enum Result(val id: Int, val name: String):
  case Win extends Result(1, "Victory")
  case Draw extends Result(2, "Draw")
  case Loss extends Result(3, "Defeat")

object Result:
  val byId = values.mapBy(_.id)
  val idList = values.map(_.id)

enum Phase(val id: Int, val name: String):
  case Opening extends Phase(1, "Opening")
  case Middle extends Phase(2, "Middlegame")
  case End extends Phase(3, "Endgame")

object Phase:
  val byId = values.mapBy(_.id)
  def of(div: chess.Division, ply: Ply): Phase =
    div.middle.fold[Phase](Opening):
      case m if ply < m => Opening
      case _ =>
        div.end.fold[Phase](Middle):
          case e if ply < e => Middle
          case _ => End

enum Castling(val id: Int, val name: String):
  case Kingside extends Castling(1, "Kingside castling")
  case Queenside extends Castling(2, "Queenside castling")
  case None extends Castling(3, "No castling")
object Castling:
  val byId = values.mapBy(_.id)
  def fromMoves(moves: Iterable[SanStr]) =
    SanStr.raw(moves).find(_.startsWith("O")) match
      case Some("O-O") => Kingside
      case Some("O-O-O") => Queenside
      case _ => None

enum QueenTrade(val id: Boolean, val name: String):
  case Yes extends QueenTrade(true, "Queen trade")
  case No extends QueenTrade(false, "No queen trade")
object QueenTrade:
  def apply(v: Boolean): QueenTrade = if v then Yes else No

enum Blur(val id: Boolean, val name: String):
  case Yes extends Blur(true, "Blur")
  case No extends Blur(false, "No blur")
object Blur:
  def apply(v: Boolean): Blur = if v then Yes else No
