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
    // cpl: Option[Grouped[Numbers]],
    // movetime: Grouped[Numbers],
    // luck: Option[Grouped[Ratio]],
    // opportunism: Option[Grouped[Ratio]],
    // nbMoves: Grouped[Int],
    result: Result,
    status: Status,
    finalPhase: Phase,
    ratingDiff: Int,
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
  eval: Option[Int],
  mate: Option[Int],
  cpl: Option[Int],
  nag: Option[Nag])

case class Opponent(rating: Int, strength: RelativeStrength)

sealed abstract class Nag(val id: Int)
object Nag {
  object Inaccuracy extends Nag(1)
  object Mistake extends Nag(2)
  object Blunder extends Nag(3)
  val all = List(Inaccuracy, Mistake, Blunder)
}

sealed abstract class Result(val id: Int)
object Result {
  object Win extends Result(1)
  object Draw extends Result(2)
  object Loss extends Result(3)
  val all = List(Win, Draw, Loss)
}

sealed abstract class Phase(val id: Int)
object Phase {
  object Opening extends Phase(1)
  object Middle extends Phase(2)
  object End extends Phase(3)
  val all = List(Opening, Middle, End)
}

sealed abstract class RelativeStrength(val id: Int)
object RelativeStrength {
  object MuchWeaker extends RelativeStrength(10)
  object Weaker extends RelativeStrength(20)
  object Equal extends RelativeStrength(30)
  object Stronger extends RelativeStrength(40)
  object MuchStronger extends RelativeStrength(50)
  val all = List(MuchWeaker, Weaker, Equal, Stronger, MuchStronger)
  def apply(diff: Int) = diff match {
    case d if d < -300 => MuchWeaker
    case d if d < -100 => Weaker
    case d if d > 100  => Stronger
    case d if d > 300  => MuchStronger
    case _             => Equal
  }
}

case class ByMovetime[A](values: Vector[A])

object ByMovetime {
  type Tenths = Int
  val uppers = List[Tenths](10, 20, 30, 50, 100, 200, 600, Int.MaxValue)
  val vectorSize = uppers.size
  val indicesVector = 0.to(vectorSize - 1).toVector
  private def upperOf(mt: Tenths) = uppers.indexWhere(_ >= mt)
  def accumulate(data: List[Tenths]): ByMovetime[Int] = ByMovetime {
    data.foldLeft(Vector.fill(vectorSize)(0)) {
      case (acc, time) =>
        val index = upperOf(time)
        acc.updated(index, acc(index) + 1)
    }
  }
  def numbers(data: List[(Tenths, Int)]): ByMovetime[Numbers] = ByMovetime {
    val hash = data.foldLeft(Map.empty[Tenths, List[Int]]) {
      case (acc, (time, v)) =>
        val index = upperOf(time)
        acc.updated(index, v :: ~acc.get(index))
    }
    indicesVector.map { i =>
      hash get i flatMap Numbers.apply getOrElse Numbers.empty
    }
  }
}

case class ByPhase[A](opening: A, middle: Option[A], end: Option[A], all: A)

case class ByPieceRole[A](pawn: Option[A], knight: Option[A], bishop: Option[A], rook: Option[A], queen: Option[A], king: Option[A])

object ByPieceRole {
  import chess.{ Knight, Bishop, Rook, Queen, King, Pawn }
  def empty[A] = ByPieceRole[A](none[A], none[A], none[A], none[A], none[A], none[A])
  def pgnMoveToRole(pgn: String): Role = pgn.head match {
    case 'N'       => Knight
    case 'B'       => Bishop
    case 'R'       => Rook
    case 'Q'       => Queen
    case 'K' | 'O' => King
    case _         => Pawn
  }
  private def inc(v: Option[Int]) = Some(1 + (v getOrElse 0))
  def accumulate(pgnMoves: PgnMoves): ByPieceRole[Int] = pgnMoves.foldLeft(empty[Int]) {
    case (acc, pgnMove) => pgnMoveToRole(pgnMove) match {
      case Pawn   => acc.copy(pawn = inc(acc.pawn))
      case Knight => acc.copy(knight = inc(acc.knight))
      case Bishop => acc.copy(bishop = inc(acc.bishop))
      case Rook   => acc.copy(rook = inc(acc.rook))
      case Queen  => acc.copy(queen = inc(acc.queen))
      case King   => acc.copy(king = inc(acc.king))
    }
  }
  def numbers(data: List[(String, Int)]): ByPieceRole[Numbers] = {
    val hash = data.foldLeft(Map.empty[Role, List[Int]]) {
      case (acc, (pgn, v)) =>
        val role = pgnMoveToRole(pgn)
        acc.updated(role, v :: ~acc.get(role))
    }.mapValues(Numbers.apply)
    ByPieceRole(
      pawn = hash get Pawn flatten,
      knight = hash get Knight flatten,
      bishop = hash get Bishop flatten,
      rook = hash get Rook flatten,
      queen = hash get Queen flatten,
      king = hash get King flatten)
  }
}

case class ByPositionQuality[A](losing: Option[A], bad: Option[A], equal: Option[A], good: Option[A], winning: Option[A])

object ByPositionQuality {
  def empty[A] = ByPositionQuality[A](none[A], none[A], none[A], none[A], none[A])
  sealed trait Quality
  case object Losing extends Quality
  case object Bad extends Quality
  case object Equal extends Quality
  case object Good extends Quality
  case object Winning extends Quality
  def evaluate(color: Color, cp: Int) = color.fold(cp, -cp) match {
    case a if a <= -500 => Losing
    case a if a <= -200 => Bad
    case a if a >= 200  => Good
    case a if a >= 500  => Winning
    case _              => Equal
  }
  private def inc(v: Option[Int]) = Some(1 + (v getOrElse 0))
  def accumulate(cps: List[Int], color: Color): ByPositionQuality[Int] = cps.foldLeft(empty[Int]) {
    case (acc, cp) => evaluate(color, cp) match {
      case Losing  => acc.copy(losing = inc(acc.losing))
      case Bad     => acc.copy(bad = inc(acc.bad))
      case Equal   => acc.copy(equal = inc(acc.equal))
      case Good    => acc.copy(good = inc(acc.good))
      case Winning => acc.copy(winning = inc(acc.winning))
    }
  }
}

case class Grouped[A](
  byPhase: ByPhase[A],
  byMovetime: ByMovetime[A],
  byPieceRole: ByPieceRole[A],
  byPositionQuality: Option[ByPositionQuality[A]])

case class Numbers(size: Int, mean: Double, median: Double, deviation: Double) {

  def isEmpty = size == 0
}

object Numbers {
  def empty = Numbers(0, 0, 0, 0)
  def apply(nbs: List[Int]): Option[Numbers] = nbs.toNel map { nel =>
    Numbers(
      size = nel.size,
      mean = Math.mean(nel),
      median = Math.median(nel),
      deviation = Math.deviation(nel))
  }
}

case class Ratio(n: Int, d: Int) // n/d
