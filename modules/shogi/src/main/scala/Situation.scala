package shogi

import cats.data.Validated
import cats.implicits._

import format.usi.Usi

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val moves: Map[Pos, List[Move]] =
    actors
      .collect {
        case actor if actor.moves.nonEmpty => actor.pos -> actor.moves
      }
      .to(Map)

  lazy val destinations: Map[Pos, List[Pos]] = moves.view.mapValues { _ map (_.dest) }.to(Map)

  def drops: Option[List[Pos]] =
    board.variant match {
      case v if v.hasHandData => v possibleDrops this
      case _                  => None
    }

  lazy val kingPos: Option[Pos] = board kingPosOf color

  lazy val check: Boolean = board check color

  def checkSquare = if (check) kingPos else None

  def history = board.history

  def checkMate: Boolean = board.variant checkmate this

  def staleMate: Boolean = board.variant staleMate this

  def autoDraw: Boolean = board.autoDraw || board.variant.specialDraw(this)

  def opponentHasInsufficientMaterial: Boolean = board.variant.opponentHasInsufficientMaterial(this)

  def perpetualCheck: Boolean = history perpetualCheck

  def variantEnd = board.variant specialEnd this

  def impasse = board.variant impasse this

  // impasse isn't here to allow studies to continue even after impasse
  def end: Boolean = checkMate || staleMate || autoDraw || perpetualCheck || variantEnd

  def winner: Option[Color] = board.variant.winner(this)

  def playable(strict: Boolean): Boolean =
    (board valid strict) && !end && !copy(color = !color).check

  def playableNoImpasse(strict: Boolean): Boolean =
    playable(strict) && !impasse

  lazy val status: Option[Status] =
    if (checkMate) Status.Mate.some
    else if (variantEnd) Status.VariantEnd.some
    else if (staleMate) Status.Stalemate.some
    else if (impasse) Status.Impasse27.some
    else if (perpetualCheck) Status.PerpetualCheck.some
    else if (autoDraw) Status.Draw.some
    else none

  def move(from: Pos, to: Pos, promotion: Boolean): Validated[String, Move] =
    board.variant.move(this, from, to, promotion)

  def move(usi: Usi.Move): Validated[String, Move] =
    board.variant.move(this, usi.orig, usi.dest, usi.promotion)

  def drop(role: Role, pos: Pos): Validated[String, Drop] =
    board.variant.drop(this, role, pos)

  def drop(usi: Usi.Drop): Validated[String, Drop] =
    board.variant.drop(this, usi.role, usi.pos)

  def withHistory(history: History) =
    copy(
      board = board withHistory history
    )

  def withVariant(variant: shogi.variant.Variant) =
    copy(
      board = board withVariant variant
    )

  def unary_! = copy(color = !color)
}

object Situation {

  def apply(variant: shogi.variant.Variant): Situation = Situation(Board init variant, Sente)
}
