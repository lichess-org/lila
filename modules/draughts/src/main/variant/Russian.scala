package draughts
package variant

import scala.annotation.tailrec
import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer

case object Russian extends Variant(
  id = 11,
  gameType = 25,
  key = "russian",
  name = "Russian",
  shortName = "Russian",
  title = "Russian draughts",
  standardInitialPosition = false,
  boardSize = Board.D64
) {

  val pieces = Variant.symmetricThreeRank(Vector(Man, Man, Man, Man), boardSize)
  val initialFen = "W:W21,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1"
  val startingPosition = StartingPosition("---", initialFen, "", "Initial position".some)
  override val openings = OpeningTable.categoriesIDF
  override val openingTables = List(OpeningTable.tableIDF)

  def captureDirs = Standard.captureDirs
  def moveDirsColor = Standard.moveDirsColor
  def moveDirsAll = Standard.moveDirsAll

  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {
    val captures: Map[Pos, List[Move]] = situation.actors.collect {
      case actor if actor.getCaptures(finalSquare).nonEmpty =>
        actor.pos -> actor.getCaptures(finalSquare)
    }(breakOut)

    if (captures.nonEmpty) captures
    else situation.actors.collect {
      case actor if actor.noncaptures.nonEmpty =>
        actor.pos -> actor.noncaptures
    }(breakOut)
  }

  def promoteOrSame(m: Move): Move =
    maybePromote(m) getOrElse m

  override def shortRangeCaptures(actor: Actor, finalSquare: Boolean): List[Move] = {
    val buf = new ArrayBuffer[Move]
    val color = actor.color

    def walkCaptures(walkDir: Direction, curBoard: Board, curPos: PosMotion, firstSquare: Option[PosMotion], firstBoard: Option[Board], allSquares: List[PosMotion], allTaken: List[PosMotion]): Int =
      walkDir._2(curPos).fold(0) {
        nextPos =>
          curBoard(nextPos) match {
            case Some(captPiece) if captPiece.isNot(color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos).fold(0) { newBoard =>
                    val promotion = promotablePos(landingPos, color)
                    val boardAfter = promotion.fold(newBoard promote landingPos, none) getOrElse newBoard
                    val newSquares = landingPos :: allSquares
                    val newTaken = nextPos :: allTaken
                    val opposite = Variant.oppositeDirs(walkDir._1)
                    val extraCaptures = captureDirs.foldLeft(0) {
                      case (total, captDir) =>
                        if (captDir._1 == opposite) total
                        else {
                          total + promotion.fold(
                            innerLongRangeCaptures(buf, actor, boardAfter, landingPos, captDir, finalSquare, firstSquare.getOrElse(landingPos).some, firstBoard.getOrElse(boardAfter).some, newSquares, newTaken),
                            walkCaptures(captDir, boardAfter, landingPos, firstSquare.getOrElse(landingPos).some, firstBoard.getOrElse(boardAfter).some, newSquares, newTaken)
                          )
                        }
                    }
                    if (extraCaptures == 0) {
                      val newMove =
                        if (finalSquare) actor.move(landingPos, boardAfter.withoutGhosts, newSquares, newTaken)
                        else promoteOrSame(actor.move(firstSquare.getOrElse(landingPos), firstBoard.getOrElse(boardAfter), newSquares, newTaken))
                      buf += newMove
                    }
                    extraCaptures + 1
                  }
                case _ => 0
              }
            case _ => 0
          }
      }

    captureDirs.foreach {
      walkCaptures(_, actor.board, actor.pos, None, None, Nil, Nil)
    }
    buf.toList
  }

  override def longRangeCaptures(actor: Actor, finalSquare: Boolean): List[Move] = {
    val buf = new ArrayBuffer[Move]
    captureDirs.foreach {
      innerLongRangeCaptures(buf, actor, actor.board, actor.pos, _, finalSquare, None, None, Nil, Nil)
    }
    buf.toList
  }

  private def innerLongRangeCaptures(buf: ArrayBuffer[Move], actor: Actor, initBoard: Board, initPos: PosMotion, initDir: Direction, finalSquare: Boolean, initFirstSquare: Option[PosMotion], initFirstBoard: Option[Board], initAllSquares: List[PosMotion], initAllTaken: List[PosMotion]): Int = {

    @tailrec
    def walkUntilCapture(walkDir: Direction, curBoard: Board, curPos: PosMotion, firstSquare: Option[PosMotion], firstBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos]): Int =
      walkDir._2(curPos) match {
        case Some(nextPos) =>
          curBoard(nextPos) match {
            case None =>
              curBoard.move(curPos, nextPos) match {
                case Some(boardAfter) =>
                  walkUntilCapture(walkDir, boardAfter, nextPos, firstSquare, firstBoard, allSquares, allTaken)
                case _ => 0
              }
            case Some(captPiece) if captPiece.isNot(actor.color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos) match {
                    case Some(boardAfter) =>
                      walkAfterCapture(walkDir, boardAfter, landingPos, firstSquare, firstBoard, allSquares, nextPos :: allTaken, true, 0)
                    case _ => 0
                  }
                case _ => 0
              }
            case _ => 0
          }
        case _ => 0
      }

    def walkAfterCapture(walkDir: Direction, curBoard: Board, curPos: PosMotion, firstSquare: Option[PosMotion], firstBoard: Option[Board], allSquares: List[Pos], newTaken: List[Pos], justTaken: Boolean, currentCaptures: Int): Int = {
      val newSquares = curPos :: allSquares
      val opposite = Variant.oppositeDirs(walkDir._1)
      val extraCaptures = captureDirs.foldLeft(0) {
        case (total, captDir) =>
          if (captDir._1 == opposite) total
          else total + walkUntilCapture(captDir, curBoard, curPos, firstSquare.getOrElse(curPos).some, firstBoard.getOrElse(curBoard).some, newSquares, newTaken)
      }
      val moreExtraCaptures = walkDir._2(curPos) match {
        case Some(nextPos) =>
          curBoard.move(curPos, nextPos) match {
            case Some(boardAfter) =>
              walkAfterCapture(walkDir, boardAfter, nextPos, firstSquare, firstBoard, allSquares, newTaken, false, currentCaptures + extraCaptures)
            case _ => 0
          }
        case _ => 0
      }
      val totalCaptures = currentCaptures + extraCaptures + moreExtraCaptures
      if (totalCaptures == 0) {
        if (finalSquare)
          buf += actor.move(curPos, curBoard.withoutGhosts, newSquares, newTaken)
        else
          buf += actor.move(firstSquare.getOrElse(curPos), firstBoard.getOrElse(curBoard), newSquares, newTaken)
      }
      if (justTaken) totalCaptures + 1
      else totalCaptures
    }

    walkUntilCapture(initDir, initBoard, initPos, initFirstSquare, initFirstBoard, initAllSquares, initAllTaken)
  }

  override def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], remainingCaptures: Int): Board = {
    if (remainingCaptures > 0) board
    else {
      val whiteActors = board.actorsOf(Color.White)
      val blackActors = board.actorsOf(Color.Black)
      val whiteKings = whiteActors.count(_.piece is King)
      val blackKings = blackActors.count(_.piece is King)
      val whitePieces = whiteActors.size
      val blackPieces = blackActors.size
      def loneKing(strongPieces: Int, strongKings: Int, weakKing: Actor) =
        strongPieces == 3 && strongKings >= 1 && weakKing.onLongDiagonal
      val whiteLoneKing =
        if (whiteKings == 1 && whitePieces == 1 && blackKings >= 1) {
          loneKing(blackPieces, blackKings, whiteActors.head)
        } else false
      val blackLoneKing =
        if (blackKings == 1 && blackPieces == 1 && whiteKings >= 1) {
          loneKing(whitePieces, whiteKings, blackActors.head)
        } else false
      if (whiteLoneKing || blackLoneKing) {
        board updateHistory { h =>
          // "abuse" kingmove counter to count the amount of moves made on the long
          // diagonal by the side with a lone king against 3 (see 7.2.7)
          h.withKingMove(Color(whiteLoneKing), None, true)
        } withoutGhosts
      } else board.withoutGhosts
    }
  }

  override def maxDrawingMoves(board: Board): Option[Int] = {
    val whiteActors = board.actorsOf(Color.White)
    val blackActors = board.actorsOf(Color.Black)
    val whiteKings = whiteActors.count(_.piece is King)
    val blackKings = blackActors.count(_.piece is King)
    val whitePieces = whiteActors.size
    val blackPieces = blackActors.size

    def singleKing(strongPieces: Int, strongKings: Int, weakKing: Actor, weakColor: Color) = {
      // weak side:   pieces == 1, kings == 1
      // strong side: pieces <= 2, kings >= 1
      //    7.2.8 => 5
      // strong side: pieces == 3, kings >= 1
      //    weak side on long diagonal => 7.2.7 => 5
      // strong side: pieces >= 3, kings == pieces
      //    7.2.4 => 15
      // strong side: kings >= 1
      //    7.2.5 => 15
      if (strongPieces <= 2 && strongKings >= 1) Some(10) // 7.2.8
      else if (strongPieces == 3 && strongKings >= 1 && weakKing.onLongDiagonal) {
        // 7.2.7: only draw after 5 kingmoves on the long diagonal have been recorded
        if (board.history.kingMoves(weakColor) >= 10) Some(10)
        else Some(30)
      } else if (strongPieces >= 3 && strongKings == strongPieces) Some(30) // 7.2.4
      else Some(30) // 7.2.5
    }
    val singleKingDraw =
      if (whiteKings == 1 && whitePieces == 1 && blackKings >= 1) {
        singleKing(blackPieces, blackKings, whiteActors.head, Color.white)
      } else if (blackKings == 1 && blackPieces == 1 && whiteKings >= 1) {
        singleKing(whitePieces, whiteKings, blackActors.head, Color.black)
      } else None

    if (singleKingDraw.isDefined) singleKingDraw
    else if (blackPieces == blackKings && whitePieces == whiteKings) {
      // 7.2.6
      val totalPieces = blackPieces + whitePieces
      if (totalPieces == 6 || totalPieces == 7) Some(60)
      else if (totalPieces == 4 || totalPieces == 5) Some(30)
      else None
    } else None
  }

  /**
   * Update position hashes for Russian drawing rules (https://fmjd64.org/rules-of-the-game/):
   * 7.2.3. If three (or more) times the same position is repeated, and each time the same player having to move.
   * 7.2.4. If a player has three kings (or more) against a single enemy king, the game is drawn if his 15th move does not capture the enemy king
   *        (counting from the time of establishing the correlation of forces).
   * 7.2.5. If within 15 moves the players made ​​moves only kings without moving of men and not making the capture.
   * 7.2.6. If the position in which the both opponents having kings have not changed the balance of pieces (ie, there was no capture and man did not become a king) for:
   *          – To 4-and 5-pieces endings – 30 moves;
   *          – In 6, and 7-pieces endings – 60 moves.
   * 7.2.7. If a player having in the party three kings, two kings and one man, one king and two men against one enemy king, located on the long diagonal, his 5th move will not be able to achieve a winning position.
   * 7.2.8. If a player having in the party two kings, one king and man, one king against enemy king to their 5th move will not be able to achieve a winning position.
   * 7.2.9. ... excluding case when the game is obvious and the player can continue to demonstrate the victory :S ...
   */
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = {
    val newHash = Hash(Situation(board, !move.piece.color))
    maxDrawingMoves(board) match {
      case Some(drawingMoves) =>
        if (drawingMoves == 30 && (move.captures || move.piece.isNot(King) || move.promotes))
          newHash // 7.2.5 and 7.2.4 both reset on capture or non-king move (in the latter case because the game is over, or it switches to 7.2.8). promotion check is included to prevent that a move promoting a man is counted as a king move
        else if (drawingMoves == 60 && (move.captures || move.promotes))
          newHash // 7.2.6 resets on capture or promotion (30 move case overlaps with previous condition)
        else // 7.2.7 is unclear - we count total moves on long diagonal from start of piece configuration, so reentering long diagonal enough times before ply 30 still draws (leaving the diagonal is dumb anyway)
          newHash ++ hash // 7.2.8 never resets once activated
      case _ => newHash
    }
  }

  override def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 12) &&
      !menOnPromotionRank(board, color)
  }
}