package draughts

import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec

case class Actor(
    piece: Piece,
    pos: Pos,
    board: Board
) {

  import Actor._

  lazy val validMoves: List[Move] = if (captures.nonEmpty) captures else noncaptures
  lazy val allMoves: List[Move] = captures ::: noncaptures
  lazy val noncaptures: List[Move] = noncaptureMoves()
  lazy val captures: List[Move] = captureMoves(false)

  lazy val allDestinations: List[Pos] = allMoves map (_.dest)

  /**
   * Same as captures, but giving the final destination square instead of the first.
   */
  lazy val capturesFinal: List[Move] = captureMoves(true)

  private def noncaptureMoves(): List[Move] = piece.role match {
    case Man => shortRangeMoves(dirsOfColor)
    case King =>
      if (board.variant.frisian && board.history.kingMoves(color) >= 3 && board.history.kingMoves.kingPos(color).fold(true)(_ == pos)) Nil
      else longRangeMoves(dirsAll)
    case _ => Nil
  }

  private def captureMoves(finalSquare: Boolean): List[Move] = piece.role match {
    case Man => shortRangeCaptures(if (board.variant.frisian) dirsAllFrisian else dirsAll, finalSquare)
    case King => longRangeCaptures(if (board.variant.frisian) dirsAllFrisian else dirsAll, finalSquare)
    case _ => Nil
  }

  def color: Color = piece.color
  def is(c: Color): Boolean = c == piece.color
  def is(p: Piece): Boolean = p == piece

  private def maybePromote(m: Move): Option[Move] =
    if (m.dest.y == m.color.promotableManY)
      (m.after promote m.dest) map { b2 =>
        m.copy(after = b2, promotion = Some(King))
      }
    else Some(m)

  private def shortRangeMoves(dirs: Directions): List[Move] =
    dirs flatMap { _._2(pos) } flatMap { to =>
      board.pieces.get(to) match {
        case None => board.move(pos, to) map { move(to, _, None, None) } flatMap maybePromote
        case Some(pc) => Nil
      }
    }

  //Speed critical function
  private def shortRangeCaptures(dirs: Directions, finalSquare: Boolean): List[Move] = {
    val buf = new ArrayBuffer[Move]

    var bestValue = 0f
    var bestLength = 0

    def walkCaptures(walkDir: Direction, curBoard: Board, curPos: Pos, destPos: Option[Pos], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos]): Unit =
      walkDir._2(curPos).fold() {
        nextPos =>
          curBoard(nextPos) match {
            case Some(captPiece) if (captPiece isNot color) && (captPiece isNot GhostMan) && (captPiece isNot GhostKing) =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos).fold() {
                    boardAfter =>
                      {
                        val newSquares = landingPos :: allSquares
                        val newTaken = nextPos :: allTaken
                        val newMove =
                          if (finalSquare)
                            move(landingPos, boardAfter.withouthGhosts(), newSquares, newTaken)
                          else
                            move(destPos.getOrElse(landingPos), destBoard.getOrElse(boardAfter), newSquares, newTaken)
                        if (board.variant.frisian) {
                          val lineValue = newMove.frisianValue
                          if (lineValue > bestValue) {
                            bestValue = lineValue
                            buf.clear()
                            buf += newMove
                          } else if ((lineValue - bestValue).abs < 0.001)
                            buf += newMove
                        } else {
                          if (allTaken.lengthCompare(bestLength) > 0) {
                            bestLength = allTaken.length
                            buf.clear()
                            buf += newMove
                          } else if (allTaken.lengthCompare(bestLength) == 0)
                            buf += newMove
                        }
                        filterOpposite(dirs, walkDir).foreach {
                          captDir =>
                            walkCaptures(captDir, boardAfter, landingPos, destPos.getOrElse(landingPos).some, destBoard.getOrElse(boardAfter).some, newSquares, newTaken)
                        }
                      }
                  }
                case _ => ()
              }
            case _ => ()
          }
      }

    dirs.foreach {
      walkDir =>
        walkCaptures(walkDir, board, pos, None, None, Nil, Nil)
    }

    buf.flatMap {
      m =>
        if (finalSquare || m.capture.getOrElse(Nil).lengthCompare(1) == 0)
          maybePromote(m)
        else
          Some(m)
    } toList
  }

  private def longRangeMoves(dirs: Directions): List[Move] = {
    val buf = new ArrayBuffer[Move]

    @tailrec
    def addAll(p: Pos, dir: Direction): Unit = {
      dir._2(p) match {
        case None => () // past end of board
        case Some(to) => board.pieces.get(to) match {
          case None =>
            board.move(pos, to).foreach { buf += move(to, _, None, None) }
            addAll(to, dir)
          case Some(pc) => ()
        }
      }
    }

    dirs foreach { addAll(pos, _) }
    buf.toList

  }

  //Speed critical function
  private def longRangeCaptures(dirs: Directions, finalSquare: Boolean): List[Move] = {
    val buf = new ArrayBuffer[Move]

    var bestValue = 0f
    var bestLength = 0

    def walkUntilCapture(walkDir: Direction, curBoard: Board, curPos: Pos, destPos: Option[Pos], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos]): Unit =
      walkDir._2(curPos).fold() {
        nextPos =>
          curBoard(nextPos) match {
            case None =>
              curBoard.move(curPos, nextPos).fold() {
                boardAfter =>
                  walkUntilCapture(walkDir, boardAfter, nextPos, destPos, destBoard, allSquares, allTaken)
              }
            case Some(captPiece) if (captPiece isNot color) && (captPiece isNot GhostMan) && (captPiece isNot GhostKing) =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos).fold() {
                    boardAfter =>
                      walkAfterCapture(walkDir, boardAfter, landingPos, destPos, destBoard, allSquares, nextPos :: allTaken)
                  }
                case _ => ()
              }
            case _ => ()
          }
      }

    def walkAfterCapture(walkDir: Direction, curBoard: Board, curPos: Pos, destPos: Option[Pos], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos]): Unit = {
      val newSquares = curPos :: allSquares
      val newMove =
        if (finalSquare)
          move(curPos, curBoard.withouthGhosts(), newSquares, allTaken)
        else
          move(destPos.getOrElse(curPos), destBoard.getOrElse(curBoard), newSquares, allTaken)
      if (board.variant.frisian) {
        val lineValue = newMove.frisianValue
        if (lineValue > bestValue) {
          bestValue = lineValue
          buf.clear()
          buf += newMove
        } else if ((lineValue - bestValue).abs < 0.001)
          buf += newMove
      } else {
        if (allTaken.lengthCompare(bestLength) > 0) {
          bestLength = allTaken.length
          buf.clear()
          buf += newMove
        } else if (allTaken.lengthCompare(bestLength) == 0)
          buf += newMove
      }
      filterOpposite(dirs, walkDir).foreach {
        captDir =>
          walkUntilCapture(captDir, curBoard, curPos, destPos.getOrElse(curPos).some, destBoard.getOrElse(curBoard).some, newSquares, allTaken)
      }
      walkDir._2(curPos).fold() {
        nextPos =>
          if (curBoard(nextPos).isEmpty)
            curBoard.move(curPos, nextPos).fold() {
              boardAfter =>
                walkAfterCapture(walkDir, boardAfter, nextPos, destPos, destBoard, allSquares, allTaken)
            }
      }
    }

    dirs.foreach {
      initialDir =>
        walkUntilCapture(initialDir, board, pos, None, None, Nil, Nil)
    }

    buf.toList
  }

  private def filterOpposite(dirs: Directions, dir: Direction): Directions = {
    val opposite = dir._1 match {
      case UpLeft => DownRight
      case DownLeft => UpRight
      case UpRight => DownLeft
      case DownRight => UpLeft
      case Up => Down
      case Down => Up
      case Left => Right
      case _ => Left
    }
    dirs filter { _._1 != opposite }
  }

  private lazy val dirsOfColor = getDirsOfColor(color)
  private lazy val dirsAll = getDirsAll()
  private lazy val dirsAllFrisian = getDirsAllFrisian()

  private def move(
    dest: Pos,
    after: Board,
    /* Single capture or none */
    capture: Option[Pos],
    taken: Option[Pos]
  ) = Move(
    piece = piece,
    orig = pos,
    dest = dest,
    situationBefore = Situation(board, piece.color),
    after = after,
    capture = capture match {
      case Some(capt) => List(capt).some
      case _ => None
    },
    taken = taken match {
      case Some(take) => List(take).some
      case _ => None
    },
    promotion = None
  )

  private def move(
    /** Destination square of the move */
    dest: Pos,
    /** Board after this move is made */
    after: Board,
    /** Chained captures (1x2x3) */
    capture: List[Pos],
    /** Pieces taken from the board */
    taken: List[Pos]
  ) = Move(
    piece = piece,
    orig = pos,
    dest = dest,
    situationBefore = Situation(board, piece.color),
    after = after,
    capture = capture.some,
    taken = taken.some,
    promotion = None
  )

  private def history = board.history
}

object Actor {

  def getDirsOfColor(color: Color): Directions = color.fold(List((UpLeft, _.moveUpLeft), (UpRight, _.moveUpRight)), List((DownLeft, _.moveDownLeft), (DownRight, _.moveDownRight)))
  def getDirsAll(): Directions = List((UpLeft, _.moveUpLeft), (UpRight, _.moveUpRight), (DownLeft, _.moveDownLeft), (DownRight, _.moveDownRight))
  def getDirsAllFrisian(): Directions = List((UpLeft, _.moveUpLeft), (UpRight, _.moveUpRight), (Up, _.moveUp), (DownLeft, _.moveDownLeft), (DownRight, _.moveDownRight), (Down, _.moveDown), (Left, _.moveLeft), (Right, _.moveRight))

  val UpLeft = 1
  val UpRight = 2
  val DownLeft = 3
  val DownRight = 4
  val Up = 5
  val Down = 6
  val Left = 7
  val Right = 8

}