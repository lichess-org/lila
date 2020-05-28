package draughts
package variant

import scala.annotation.tailrec
import scala.collection.breakOut
import scalaz.Validation.FlatMap._

// Correctness depends on singletons for each variant ID
abstract class Variant private[variant] (
    val id: Int,
    val gameType: Int,
    val key: String,
    val name: String,
    val shortName: String,
    val title: String,
    val standardInitialPosition: Boolean,
    val boardSize: Board.BoardSize
) {

  def pieces: Map[Pos, Piece]
  def captureDirs: Directions
  def moveDirsColor: Map[Color, Directions]
  def moveDirsAll: Directions

  def standard = this == Standard
  def frisian = this == Frisian
  def frysk = this == Frysk
  def antidraughts = this == Antidraughts
  def breakthrough = this == Breakthrough
  def russian = this == Russian
  def fromPosition = this == FromPosition

  def frisianVariant = frisian || frysk
  def exotic = !standard

  def initialFen = format.Forsyth.initial

  def isValidPromotion(promotion: Option[PromotableRole]) = promotion match {
    case None => true
    case Some(King) => true
    case _ => false
  }

  def getCaptureValue(board: Board, taken: List[Pos]) = taken.length
  def getCaptureValue(board: Board, taken: Pos) = 1

  def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {
    var bestLineValue = 0
    var captureMap = Map[Pos, List[Move]]()
    for (actor <- situation.actors) {
      val capts = if (finalSquare) actor.capturesFinal else actor.captures
      if (capts.nonEmpty) {
        val lineValue = capts.head.taken.fold(0)(_.length)
        if (lineValue > bestLineValue) {
          bestLineValue = lineValue
          captureMap = Map(actor.pos -> capts)
        } else if (lineValue == bestLineValue)
          captureMap = captureMap + (actor.pos -> capts)
      }
    }

    if (captureMap.nonEmpty) captureMap
    else situation.actors.collect {
      case actor if actor.noncaptures.nonEmpty =>
        actor.pos -> actor.noncaptures
    }(breakOut)
  }

  def validMovesFrom(situation: Situation, pos: Pos, finalSquare: Boolean = false): List[Move] = situation.actorAt(pos) match {
    case Some(actor) => {
      val captures = if (finalSquare) actor.capturesFinal else actor.captures
      if (captures.nonEmpty) captures
      else actor.noncaptures
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

  def move(situation: Situation, from: Pos, to: Pos, promotion: Option[PromotableRole], finalSquare: Boolean = false, forbiddenUci: Option[List[String]] = None, captures: Option[List[Pos]] = None, partialCaptures: Boolean = false): Valid[Move] = {

    // Find the move in the variant specific list of valid moves
    def findMove(from: Pos, to: Pos) = {
      val moves = validMovesFrom(situation, from, finalSquare)
      val exactMatch = moves.find { m =>
        if (forbiddenUci.fold(false)(_.contains(m.toUci.uci))) false
        else m.dest == to && captures.fold(true)(m.capture.contains)
      }
      if (exactMatch.isEmpty && partialCaptures && captures.isDefined) {
        moves.find { m =>
          if (forbiddenUci.fold(false)(_.contains(m.toUci.uci))) false
          else m.capture.isDefined && captures.fold(false) { capts =>
            m.capture.get.endsWith(capts)
          }
        }
      } else exactMatch
    }

    for {
      actor ← situation.board.actors get from toValid "No piece on " + from
      _ ← actor.validIf(actor is situation.color, "Not my piece on " + from)
      m1 ← findMove(from, to) toValid "Piece on " + from + " cannot move to " + to
      m2 ← m1 withPromotion promotion toValid "Piece on " + from + " cannot promote to " + promotion
      m3 <- m2 validIf (isValidPromotion(promotion), "Cannot promote to " + promotion + " in this game mode")
    } yield m3

  }

  def promotablePos(pos: PosMotion, color: Color) =
    pos.y == color.fold(boardSize.promotableYWhite, boardSize.promotableYBlack)

  def maybePromote(m: Move): Option[Move] =
    if (promotablePos(m.after.posAt(m.dest), m.color))
      (m.after promote m.dest) map { b2 =>
        m.copy(after = b2, promotion = Some(King))
      }
    else Some(m)

  def shortRangeCaptures(actor: Actor, finalSquare: Boolean): List[Move] = {
    val buf = new scala.collection.mutable.ArrayBuffer[Move]
    var bestCaptureValue = 0

    def walkCaptures(walkDir: Direction, curBoard: Board, curPos: PosMotion, destPos: Option[PosMotion], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos], captureValue: Int): Unit =
      walkDir._2(curPos).fold() {
        nextPos =>
          curBoard(nextPos) match {
            case Some(captPiece) if captPiece.isNot(actor.color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos).fold() { boardAfter =>
                    val newSquares = landingPos :: allSquares
                    val newTaken = nextPos :: allTaken
                    val newCaptureValue = captureValue + getCaptureValue(actor.board, nextPos)
                    if (newCaptureValue > bestCaptureValue) {
                      bestCaptureValue = newCaptureValue
                      buf.clear()
                    }
                    if (newCaptureValue == bestCaptureValue) {
                      if (finalSquare)
                        buf += actor.move(landingPos, boardAfter.withoutGhosts, newSquares, newTaken)
                      else
                        buf += actor.move(destPos.getOrElse(landingPos), destBoard.getOrElse(boardAfter), newSquares, newTaken)
                    }
                    val opposite = Variant.oppositeDirs(walkDir._1)
                    captureDirs.foreach {
                      captDir =>
                        if (captDir._1 != opposite)
                          walkCaptures(captDir, boardAfter, landingPos, destPos.getOrElse(landingPos).some, destBoard.getOrElse(boardAfter).some, newSquares, newTaken, newCaptureValue)
                    }
                  }
                case _ =>
              }
            case _ =>
          }
      }

    captureDirs.foreach {
      walkCaptures(_, actor.board, actor.pos, None, None, Nil, Nil, 0)
    }

    buf.flatMap { m =>
      if (finalSquare || m.capture.exists(_.length == 1)) maybePromote(m)
      else m.some
    } toList
  }

  def longRangeCaptures(actor: Actor, finalSquare: Boolean): List[Move] = {
    val buf = new scala.collection.mutable.ArrayBuffer[Move]
    var bestCaptureValue = 0

    // "transposition table", dramatically reduces calculation time for extreme frisian positions like W:WK50:B3,7,10,12,13,14,17,20,21,23,25,30,32,36,38,39,41,43,K47
    val cacheExtraCapts = scala.collection.mutable.LongMap.empty[Int]
    // but not enough apparently, frisian can still max out CPU with e.g. W:WK5:BK2,K4,K7,K8,K9,K10,K11,K13,K15,K16,K18,K19,K20,K21,K22,K24,K27,K29,K30,K31,K32,K33,K35,K36,K38,K40,K41,K42,K43,K44,K47,K49
    // temporary hackfix for server stability: simply abort on * wet finger * 1600 cache entries - this should be enough for any practical game position, but may lead to incorrect dests in extreme frisian analysis (soit)
    val maxCache = 1600

    @tailrec
    def walkUntilCapture(walkDir: Direction, curBoard: Board, curPos: PosMotion, destPos: Option[PosMotion], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos], captureValue: Int): Int =
      if (cacheExtraCapts.size > maxCache) 0
      else walkDir._2(curPos) match {
        case Some(nextPos) =>
          curBoard(nextPos) match {
            case None =>
              curBoard.move(curPos, nextPos) match {
                case Some(boardAfter) =>
                  walkUntilCapture(walkDir, boardAfter, nextPos, destPos, destBoard, allSquares, allTaken, captureValue)
                case _ =>
                  captureValue
              }
            case Some(captPiece) if captPiece.isNot(actor.color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos) match {
                    case Some(boardAfter) =>
                      walkAfterCapture(walkDir, boardAfter, landingPos, destPos, destBoard, allSquares, nextPos :: allTaken, captureValue + getCaptureValue(actor.board, nextPos))
                    case _ =>
                      captureValue
                  }
                case _ => captureValue
              }
            case _ => captureValue
          }
        case _ => captureValue
      }

    def walkAfterCapture(walkDir: Direction, curBoard: Board, curPos: PosMotion, destPos: Option[PosMotion], destBoard: Option[Board], allSquares: List[Pos], newTaken: List[Pos], newCaptureValue: Int): Int = {
      val captsHash = curBoard.pieces.hashCode() + walkDir._1
      val cachedExtraCapts = cacheExtraCapts.get(captsHash)
      if (cacheExtraCapts.size > maxCache) 0
      else cachedExtraCapts match {
        case Some(extraCapts) if newCaptureValue + extraCapts < bestCaptureValue =>
          // no need to calculate lines where we know they will end up too short
          newCaptureValue + extraCapts
        case _ =>
          val newSquares = curPos :: allSquares
          if (newCaptureValue > bestCaptureValue) {
            bestCaptureValue = newCaptureValue
            buf.clear()
          }
          if (newCaptureValue == bestCaptureValue) {
            if (finalSquare)
              buf += actor.move(curPos, curBoard.withoutGhosts, newSquares, newTaken)
            else
              buf += actor.move(destPos.getOrElse(curPos), destBoard.getOrElse(curBoard), newSquares, newTaken)
          }
          val opposite = Variant.oppositeDirs(walkDir._1)
          var maxExtraCapts = 0
          captureDirs.foreach { captDir =>
            if (captDir._1 != opposite) {
              val extraCapture = walkUntilCapture(captDir, curBoard, curPos, destPos.getOrElse(curPos).some, destBoard.getOrElse(curBoard).some, newSquares, newTaken, newCaptureValue) - newCaptureValue
              if (extraCapture > maxExtraCapts)
                maxExtraCapts = extraCapture
            }
          }
          walkDir._2(curPos) match {
            case Some(nextPos) =>
              curBoard.move(curPos, nextPos) match {
                case Some(boardAfter) =>
                  val extraCapture = walkAfterCapture(walkDir, boardAfter, nextPos, destPos, destBoard, allSquares, newTaken, newCaptureValue) - newCaptureValue
                  if (extraCapture > maxExtraCapts)
                    maxExtraCapts = extraCapture
                case _ =>
              }
            case _ =>
          }
          if (cachedExtraCapts.isEmpty)
            cacheExtraCapts += (captsHash, maxExtraCapts)
          newCaptureValue + maxExtraCapts
      }
    }

    captureDirs.foreach {
      walkUntilCapture(_, actor.board, actor.pos, None, None, Nil, Nil, 0)
    }

    if (cacheExtraCapts.size > maxCache) {
      logger.warn(s"longRangeCaptures aborted with ${cacheExtraCapts.size} entries for ${actor.piece} at ${actor.pos.shortKey} on ${draughts.format.Forsyth.exportBoard(actor.board)}")
    }

    buf.toList
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
    if (remainingCaptures > 0) board
    else board.withoutGhosts
  }

  protected def menOnPromotionRank(board: Board, color: Color) = {
    board.pieces.exists {
      case (pos, Piece(c, r)) if c == color && r == Man && promotablePos(board.posAt(pos), color) => true
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
  lazy val rolesByPdn: Map[Char, Role] = roles.map { r => (r.pdn, r) }(breakOut)

  override def toString = s"Variant($name)"

  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]

  override def hashCode: Int = id
}

object Variant {

  val all = List(Standard, Frisian, Frysk, Antidraughts, Breakthrough, Russian, FromPosition)
  val byId = all map { v => (v.id, v) } toMap
  val byKey = all map { v => (v.key, v) } toMap

  val allVariants = all.filter(FromPosition !=)

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
    draughts.variant.Breakthrough,
    draughts.variant.Russian
  )

  val divisionSensibleVariants: Set[Variant] = Set(
    draughts.variant.Standard,
    draughts.variant.Frisian,
    draughts.variant.Antidraughts,
    draughts.variant.Breakthrough,
    draughts.variant.Russian,
    draughts.variant.FromPosition
  )

  val UpLeft = 1
  val UpRight = 2
  val DownLeft = 3
  val DownRight = 4
  val Up = 5
  val Down = 6
  val Left = 7
  val Right = 8

  val oppositeDirs: Array[Int] = Array(
    0,
    DownRight,
    DownLeft,
    UpRight,
    UpLeft,
    Down,
    Up,
    Right,
    Left
  )

  private[variant] def symmetricFourRank(rank: IndexedSeq[Role], boardSize: Board.BoardSize): Map[Pos, Piece] = {
    (for (y ← Seq(1, 2, 3, 4, 7, 8, 9, 10); x ← 1 to 5) yield {
      boardSize.pos.posAt(x, y) map { pos =>
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

  private[variant] def symmetricThreeRank(rank: IndexedSeq[Role], boardSize: Board.BoardSize): Map[Pos, Piece] = {
    (for (y ← Seq(1, 2, 3, 6, 7, 8); x ← 1 to 4) yield {
      boardSize.pos.posAt(x, y) map { pos =>
        (pos, y match {
          case 1 => Black - rank(x - 1)
          case 2 => Black - rank(x - 1)
          case 3 => Black - rank(x - 1)
          case 6 => White - rank(x - 1)
          case 7 => White - rank(x - 1)
          case 8 => White - rank(x - 1)
        })
      }
    }).flatten.toMap
  }

  private[variant] def symmetricBackrank(rank: IndexedSeq[Role], boardSize: Board.BoardSize): Map[Pos, Piece] = {
    (for (y ← Seq(1, 10); x ← 1 to 5) yield {
      boardSize.pos.posAt(x, y) map { pos =>
        (pos, y match {
          case 1 => Black - rank(x - 1)
          case 10 => White - rank(x - 1)
        })
      }
    }).flatten.toMap
  }

}
