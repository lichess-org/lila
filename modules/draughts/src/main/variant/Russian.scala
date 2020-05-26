package draughts
package variant

import scala.collection.breakOut

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

  val pieces: Map[Pos, Piece] = Variant.symmetricThreeRank(Vector(Man, Man, Man, Man), boardSize)

  def captureDirs = Standard.captureDirs
  def moveDirsColor = Standard.moveDirsColor
  def moveDirsAll = Standard.moveDirsAll

  override val initialFen = "W:W21,22,23,24,25,26,27,28,29,30,31,32:B1,2,3,4,5,6,7,8,9,10,11,12:H0:F1"

  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {
    val captures: Map[Pos, List[Move]] = situation.actors.collect {
      case actor if actor.captures.nonEmpty =>
        actor.pos -> actor.captures
    }(breakOut)

    if (captures.nonEmpty) captures
    else situation.actors.collect {
      case actor if actor.noncaptures.nonEmpty =>
        actor.pos -> actor.noncaptures
    }(breakOut)
  }

  override def shortRangeCaptures(actor: Actor, finalSquare: Boolean): List[Move] = {
    val buf = new scala.collection.mutable.ArrayBuffer[Move]

    def walkCaptures(walkDir: Direction, curBoard: Board, curPos: PosMotion, firstSquare: Option[PosMotion], firstBoard: Option[Board], allSquares: List[PosMotion], allTaken: List[PosMotion]): Int =
      walkDir._2(curPos).fold(0) {
        nextPos =>
          curBoard(nextPos) match {
            case Some(captPiece) if captPiece.isNot(actor.color) && !captPiece.isGhost =>
              walkDir._2(nextPos) match {
                case Some(landingPos) if curBoard(landingPos).isEmpty =>
                  curBoard.taking(curPos, landingPos, nextPos).fold(0) { boardAfter =>
                    val newSquares = landingPos :: allSquares
                    val newTaken = nextPos :: allTaken
                    val opposite = Variant.oppositeDirs(walkDir._1)
                    val extraCaptures = captureDirs.foldLeft(0) {
                      case (total, captDir) =>
                        if (captDir._1 == opposite) total
                        else total + walkCaptures(captDir, boardAfter, landingPos, firstSquare.getOrElse(landingPos).some, firstBoard.getOrElse(boardAfter).some, newSquares, newTaken)
                    }
                    if (extraCaptures == 0) {
                      if (finalSquare)
                        buf += actor.move(landingPos, boardAfter.withoutGhosts, newSquares, newTaken)
                      else
                        buf += actor.move(firstSquare.getOrElse(landingPos), firstBoard.getOrElse(boardAfter), newSquares, newTaken)
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

  override def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 12) &&
      !menOnPromotionRank(board, color)
  }
}