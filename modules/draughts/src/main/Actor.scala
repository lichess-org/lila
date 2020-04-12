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
    case Man => shortRangeMoves(board.variant.moveDirsColor(color))
    case King =>
      if (board.variant.frisianVariant && board.history.kingMoves(color) >= 3 && board.history.kingMoves.kingPos(color).fold(true)(_ == pos)) Nil
      else longRangeMoves(board.variant.moveDirsAll)
    case _ => Nil
  }

  private def captureMoves(finalSquare: Boolean): List[Move] = piece.role match {
    case Man => shortRangeCaptures(board.variant.captureDirs, finalSquare)
    case King => longRangeCaptures(board.variant.captureDirs, finalSquare)
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
    var bestCaptureValue = 0

    def walkCaptures(walkDir: Direction, curBoard: Board, curPos: Pos, destPos: Option[Pos], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos], captureValue: Int): Unit =
      walkDir._2(curPos).fold() {
        nextPos =>
          curBoard(nextPos) match {
            case Some(captPiece) if (captPiece isNot color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos).fold() {
                    boardAfter =>
                      {
                        val newSquares = landingPos :: allSquares
                        val newTaken = nextPos :: allTaken
                        val newCaptureValue = captureValue + board.variant.captureValue(board, nextPos)
                        if (newCaptureValue > bestCaptureValue) {
                          bestCaptureValue = newCaptureValue
                          buf.clear()
                        }
                        if (newCaptureValue == bestCaptureValue) {
                          if (finalSquare)
                            buf += move(landingPos, boardAfter.withoutGhosts, newSquares, newTaken)
                          else
                            buf += move(destPos.getOrElse(landingPos), destBoard.getOrElse(boardAfter), newSquares, newTaken)
                        }
                        val opposite = oppositeDirs(walkDir._1)
                        dirs.foreach {
                          captDir =>
                            if (captDir._1 != opposite)
                              walkCaptures(captDir, boardAfter, landingPos, destPos.getOrElse(landingPos).some, destBoard.getOrElse(boardAfter).some, newSquares, newTaken, newCaptureValue)
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
        walkCaptures(walkDir, board, pos, None, None, Nil, Nil, 0)
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
    var bestCaptureValue = 0

    // "transposition table", dramatically reduces calculation time for extreme frisian positions like W:WK50:B3,7,10,12,13,14,17,20,21,23,25,30,32,36,38,39,41,43,K47
    val cacheExtraCapts = scala.collection.mutable.LongMap.empty[Int]
    // but not enough apparently, frisian can still max out CPU with e.g. W:WK5:BK2,K4,K7,K8,K9,K10,K11,K13,K15,K16,K18,K19,K20,K21,K22,K24,K27,K29,K30,K31,K32,K33,K35,K36,K38,K40,K41,K42,K43,K44,K47,K49
    // temporary hackfix for server stability: simply abort on * wet finger * 1200 cache entries - this should be enough for any practical game position, but may lead to incorrect dests in extreme frisian analysis (soit)
    val maxCache = 1200

    @tailrec
    def walkUntilCapture(walkDir: Direction, curBoard: Board, curPos: Pos, destPos: Option[Pos], destBoard: Option[Board], allSquares: List[Pos], allTaken: List[Pos], captureValue: Int): Int =
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
            case Some(captPiece) if (captPiece isNot color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos) match {
                    case Some(boardAfter) =>
                      walkAfterCapture(walkDir, boardAfter, landingPos, destPos, destBoard, allSquares, nextPos :: allTaken, captureValue + board.variant.captureValue(board, nextPos))
                    case _ =>
                      captureValue
                  }
                case _ => captureValue
              }
            case _ => captureValue
          }
        case _ => captureValue
      }

    def walkAfterCapture(walkDir: Direction, curBoard: Board, curPos: Pos, destPos: Option[Pos], destBoard: Option[Board], allSquares: List[Pos], newTaken: List[Pos], newCaptureValue: Int): Int = {
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
              buf += move(curPos, curBoard.withoutGhosts, newSquares, newTaken)
            else
              buf += move(destPos.getOrElse(curPos), destBoard.getOrElse(curBoard), newSquares, newTaken)
          }
          val opposite = oppositeDirs(walkDir._1)
          var maxExtraCapts = 0
          dirs.foreach {
            captDir =>
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

    dirs.foreach {
      initialDir =>
        walkUntilCapture(initialDir, board, pos, None, None, Nil, Nil, 0)
    }

    if (cacheExtraCapts.size > maxCache) {
      logger.warn(s"longRangeCaptures aborted with ${cacheExtraCapts.size} entries for ${piece} at ${pos.shortKey} on ${draughts.format.Forsyth.exportBoard(board)}")
    }

    buf.toList
  }

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

}