package draughts

import format.Uci

import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val ghosts = board.ghosts

  lazy val validMoves: Map[Pos, List[Move]] = board.variant.validMoves(this)

  lazy val allCaptures: Map[Pos, List[Move]] = board.variant.allCaptures(this)

  lazy val allMovesCaptureLength: Option[Int] =
    if (validMoves exists (_._2 exists (_.captures)))
      Some(validMoves.head._2.head.capture.fold(0)(_.length))
    else None

  lazy val allAmbiguities: Int =
    board.variant.validMoves(this, true).filter(posMoves =>
      posMoves._2.lengthCompare(1) > 0 && posMoves._2.exists(_.capture.fold(false)(_.lengthCompare(1) > 0))).foldLeft(0) {
      (ambs, pos) => ambs + calculateAmbiguitiesFrom(pos._1, pos._2)
    }

  def ambiguitiesFrom(pos: Pos): Int = calculateAmbiguitiesFrom(pos, movesFrom(pos, true))

  def ambiguitiesMove(move: Move): Int = ambiguitiesMove(move.orig, move.dest)
  def ambiguitiesMove(orig: Pos, dest: Pos): Int = calculateAmbiguitiesFrom(orig, movesFrom(orig, true).filter(_.dest == dest))

  private def calculateAmbiguitiesFrom(pos: Pos, moves: List[Move]) =
    moves.foldLeft(0) {
      (ambs, m1) => ambs + moves.exists(m2 => m1.capture.fold(none[Pos])(_.headOption) == m2.capture.fold(none[Pos])(_.headOption) && m1.situationAfter.board.pieces != m2.situationAfter.board.pieces).fold(1, 0)
    }

  def movesFrom(pos: Pos, finalSquare: Boolean = false): List[Move] = board.variant.validMovesFrom(this, pos, finalSquare)

  def captureLengthFrom(pos: Pos): Option[Int] =
    movesFrom(pos) match {
      case head :: _ => head.capture.fold(none[Int])(_.length.some)
      case _ => none
    }

  lazy val allDestinations: Map[Pos, List[Pos]] = validMoves mapValues { _ map (_.dest) }
  lazy val allCaptureDestinations: Map[Pos, List[Pos]] = allCaptures mapValues { _ map (_.dest) }

  def destinationsFrom(pos: Pos): List[Pos] = movesFrom(pos) map (_.dest)

  def actorAt(pos: Pos): Option[Actor] = board.actorAt(pos)

  def drops: Option[List[Pos]] = None

  lazy val kingPos: Option[Pos] = board kingPosOf color

  def history = board.history

  def checkMate: Boolean = board.variant checkmate this

  def autoDraw: Boolean = board.autoDraw || board.variant.specialDraw(this)

  lazy val threefoldRepetition: Boolean = board.history.threefoldRepetition

  def variantEnd = board.variant specialEnd this

  def end: Boolean = checkMate || autoDraw || variantEnd

  def winner: Option[Color] = board.variant.winner(this)

  def playable(strict: Boolean): Boolean =
    (board valid strict) && !end

  lazy val status: Option[Status] =
    if (checkMate) Status.Mate.some
    else if (variantEnd) Status.VariantEnd.some
    else if (autoDraw) Status.Draw.some
    else none

  def move(from: Pos, to: Pos, promotion: Option[PromotableRole], finalSquare: Boolean = false, forbiddenUci: Option[List[String]] = None, captures: Option[List[Pos]] = None): Valid[Move] =
    board.variant.move(this, from, to, promotion, finalSquare, forbiddenUci, captures)

  def move(uci: Uci.Move): Valid[Move] =
    board.variant.move(this, uci.orig, uci.dest, uci.promotion)

  def withHistory(history: DraughtsHistory) = copy(
    board = board withHistory history
  )

  def withVariant(variant: draughts.variant.Variant) = copy(
    board = board withVariant variant
  )

  def unary_! = copy(color = !color)
}

object Situation {

  def apply(variant: draughts.variant.Variant): Situation = Situation(Board init variant, White)
}
