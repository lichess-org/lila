package draughts
package variant

import scala.collection.breakOut
import scalaz.Validation.FlatMap._
import scalaz.Validation.failureNel

import Pos.posAt

// Correctness depends on singletons for each variant ID
abstract class Variant private[variant] (
    val id: Int,
    val gameType: Int,
    val key: String,
    val name: String,
    val shortName: String,
    val title: String,
    val standardInitialPosition: Boolean
) {

  def pieces: Map[Pos, Piece]

  def standard = this == Standard
  def frisian = this == Frisian
  def frysk = this == Frysk
  def antidraughts = this == Antidraughts
  def breakthrough = this == Breakthrough
  def fromPosition = this == FromPosition

  def frisianVariant = frisian || frysk

  def exotic = !standard

  def initialFen = format.Forsyth.initial

  protected val standardRank = Vector(Man, Man, Man, Man, Man)

  def isValidPromotion(promotion: Option[PromotableRole]) = promotion match {
    case None => true
    case Some(King) => true
    case _ => false
  }

  def allCaptures(situation: Situation): Map[Pos, List[Move]] =
    situation.actors.collect {
      case actor if actor.captures.nonEmpty =>
        actor.pos -> actor.captures
    }(breakOut)

  @inline
  def captureValue(board: Board, taken: List[Pos]) = taken.length
  @inline
  def captureValue(board: Board, taken: Pos) = 1

  def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {

    var bestLineValue = 0
    var captureMap = Map[Pos, List[Move]]()
    var captureKing = false
    for (actor <- situation.actors) {
      val capts = if (finalSquare) actor.capturesFinal else actor.captures
      if (capts.nonEmpty) {
        val lineValue = capts.head.taken.fold(0)(captureValue(situation.board, _))
        if (lineValue > bestLineValue) {
          bestLineValue = lineValue
          captureMap = Map(actor.pos -> capts)
        } else if (lineValue == bestLineValue)
          captureMap = captureMap + (actor.pos -> capts)
      }
    }

    if (captureMap.nonEmpty)
      captureMap
    else
      situation.actors.collect {
        case actor if actor.noncaptures.nonEmpty =>
          actor.pos -> actor.noncaptures
      }(breakOut)

  }

  def validMovesFrom(situation: Situation, pos: Pos, finalSquare: Boolean = false): List[Move] = situation.actorAt(pos) match {
    case Some(actor) => {
      val captures = if (finalSquare) actor.capturesFinal else actor.captures
      if (captures.nonEmpty)
        captures
      else
        actor.noncaptures
    }
    case _ => Nil
  }

  def validMoves(actor: Actor): List[Move] = {
    val captures = actor.captures
    if (captures.nonEmpty)
      captures
    else
      actor.noncaptures
  }

  /*def longRangeThreatens(board: Board, p: Pos, dir: Direction, to: Pos): Boolean = dir._2(p) exists { next =>
    next == to || (!board.pieces.contains(next) && longRangeThreatens(board, next, dir, to))
  }*/

  def move(situation: Situation, from: Pos, to: Pos, promotion: Option[PromotableRole], finalSquare: Boolean = false, forbiddenUci: Option[List[String]] = None, captures: Option[List[Pos]] = None, partialCaptures: Boolean = false): Valid[Move] = {

    // Find the move in the variant specific list of valid moves
    def findMove(from: Pos, to: Pos) = validMovesFrom(situation, from, finalSquare).find { m =>
      if (forbiddenUci.fold(false)(_.contains(m.toUci.uci))) false
      else if (m.dest == to && captures.fold(true)(m.capture.contains)) true
      else partialCaptures && m.capture.isDefined && captures.fold(false) { capts =>
        m.capture.get.endsWith(capts)
      }
    }

    for {
      actor ← situation.board.actors get from toValid "No piece on " + from
      myActor ← actor.validIf(actor is situation.color, "Not my piece on " + from)
      m1 ← findMove(from, to) toValid "Piece on " + from + " cannot move to " + to
      m2 ← m1 withPromotion promotion toValid "Piece on " + from + " cannot promote to " + promotion
      m3 <- m2 validIf (isValidPromotion(promotion), "Cannot promote to " + promotion + " in this game mode")
    } yield m3

  }

  def checkmate(situation: Situation) = situation.validMoves.isEmpty

  // In most variants, the winner is the last player to have played and there is a possibility of either a traditional
  // checkmate or a variant end condition
  def winner(situation: Situation): Option[Color] =
    if (situation.checkMate || specialEnd(situation)) Some(!situation.color) else None

  def specialEnd(situation: Situation) = false

  def specialDraw(situation: Situation) = false

  /**
   * Returns the amount of moves until a draw is reached given the material on the board.
   */
  def maxDrawingMoves(board: Board) = InsufficientWinningMaterial(board)

  // Some variants have an extra effect on the board on a move. For example, in Atomic, some
  // pieces surrounding a capture explode
  def hasMoveEffects = false

  /**
   * Applies a variant specific effect to the move. This helps decide whether a king is endangered by a move, for
   * example
   */
  def addVariantEffect(move: Move): Move = move

  /**
   * Update position hashes for standard drawing rules:
   * - The game is drawn when both players make 25 consecutive king moves without capturing.
   * - When one player has only a king left, and the other player three pieces including at least one king (three kings, two kings and a man, or one king and two men), the game is drawn after both players made 16 moves.
   * - When one player has only a king left, and the other player  player two pieces or less, including at least one king (one king, two kings, or one king and a man), the game is drawn after both players made 5 moves.
   */
  def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = {
    val newHash = Hash(Situation(board, !move.piece.color))
    maxDrawingMoves(board) match {
      case Some(drawingMoves) =>
        if (drawingMoves == 50 && (move.captures || move.piece.isNot(King) || move.promotes)) newHash //25-move rule resets on capture or non-king move. promotion check is included to prevent that a move promoting a man is counted as a king move
        else if (drawingMoves == 32 && move.captures) newHash //16 move rule resets only when another piece disappears, activating the 5-move rule
        else newHash ++ hash //5 move rule never resets once activated
      case _ => newHash
    }
  }

  /**
   * Once a move has been decided upon from the available legal moves, the board is finalized
   * This removes any reaining ghostpieces if the capture sequence has ended
   */
  def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], remainingCaptures: Int): Board = {
    if (remainingCaptures > 0)
      board
    else
      board.withoutGhosts
  }

  protected def menOnPromotionRank(board: Board, color: Color) = {
    board.pieces.exists {
      case (pos, Piece(c, r)) if c == color && r == Man && pos.y == color.promotableManY => true
      case _ => false
    }
  }

  /**
   * Checks board for valid game position
   */
  protected def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 20) &&
      (!menOnPromotionRank(board, color) || board.ghosts != 0)
  }

  def valid(board: Board, strict: Boolean) = Color.all forall validSide(board, strict)_

  val roles = List(Man, King)

  val promotableRoles: List[PromotableRole] = List(King)

  lazy val rolesByPdn: Map[Char, Role] = roles.map { r => (r.pdn, r) }(breakOut)

  def isUnmovedPawn(color: Color, pos: Pos) = pos.y == color.fold(2, 7)

  val captureDirs: Directions = List((Actor.UpLeft, _.moveUpLeft), (Actor.UpRight, _.moveUpRight), (Actor.DownLeft, _.moveDownLeft), (Actor.DownRight, _.moveDownRight))

  val moveDirsColor: Map[Color, Directions] = Map(White -> List((Actor.UpLeft, _.moveUpLeft), (Actor.UpRight, _.moveUpRight)), Black -> List((Actor.DownLeft, _.moveDownLeft), (Actor.DownRight, _.moveDownRight)))
  val moveDirsAll: Directions = moveDirsColor(White) ::: moveDirsColor(Black)

  override def toString = s"Variant($name)"

  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]

  override def hashCode: Int = id
}

object Variant {

  val all = List(Standard, Frisian, Frysk, Antidraughts, Breakthrough, FromPosition)
  val byId = all map { v => (v.id, v) } toMap
  val byKey = all map { v => (v.key, v) } toMap

  val default = Standard

  def apply(id: Int): Option[Variant] = byId get id
  def apply(key: String): Option[Variant] = byKey get key
  def orDefault(id: Int): Variant = apply(id) | default
  def orDefault(key: String): Variant = apply(key) | default

  def byName(name: String): Option[Variant] =
    all find (_.name.toLowerCase == name.toLowerCase)

  def byGameType(gameType: Int): Option[Variant] =
    all find (_.gameType == gameType)

  def exists(id: Int): Boolean = byId contains id

  val openingSensibleVariants: Set[Variant] = Set(
    draughts.variant.Standard,
    draughts.variant.Frisian,
    draughts.variant.Breakthrough
  )

  val divisionSensibleVariants: Set[Variant] = Set(
    draughts.variant.Standard,
    draughts.variant.Frisian,
    draughts.variant.Antidraughts,
    draughts.variant.Breakthrough,
    draughts.variant.FromPosition
  )

  private[variant] def symmetricFourRank(rank: IndexedSeq[Role]): Map[Pos, Piece] = {
    (for (y ← Seq(1, 2, 3, 4, 7, 8, 9, 10); x ← 1 to 5) yield {
      posAt(x, y) map { pos =>
        (pos, y match {
          case 1 => Black - rank(x - 1)
          case 2 => Black - rank(x - 1)
          case 3 => Black - rank(x - 1)
          case 4 => Black - rank(x - 1)
          case 7 => White - rank(x - 1)
          case 8 => White - rank(x - 1)
          case 9 => White - rank(x - 1)
          case 10 => White - rank(x - 1)
        })
      }
    }).flatten.toMap
  }

  private[variant] def symmetricBackrank(rank: IndexedSeq[Role]): Map[Pos, Piece] = {
    (for (y ← Seq(1, 10); x ← 1 to 5) yield {
      posAt(x, y) map { pos =>
        (pos, y match {
          case 1 => Black - rank(x - 1)
          case 10 => White - rank(x - 1)
        })
      }
    }).flatten.toMap
  }

}
