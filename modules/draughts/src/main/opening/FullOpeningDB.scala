package draughts
package opening

import format.FEN

object FullOpeningDB {

  private def all: Vector[FullOpening] = FullOpeningPart1.db ++ FullOpeningPart2.db

  lazy val byFen: collection.Map[String, FullOpening] =
    all.map { o =>
      o.fen -> o
    }(scala.collection.breakOut[Vector[_], (String, FullOpening), collection.mutable.AnyRefMap[String, FullOpening]])

  def findByFen(fen: String) = byFen get fen.split(' ').take(3).mkString(" ")

  val SEARCH_MAX_PLIES = 40

  // assumes standard initial FEN and variant
  def search(moveStrs: Traversable[String]): Option[FullOpening.AtPly] =
    draughts.Replay.boards(moveStrs take SEARCH_MAX_PLIES, None, variant.Standard).toOption.flatMap {
      _.zipWithIndex.drop(1).foldRight(none[FullOpening.AtPly]) {
        case ((board, ply), None) =>
          val fen = format.Forsyth.exportStandardPositionTurn(board, ply)
          byFen get fen map (_ atPly ply)
        case (_, found) => found
      }
    }

  def searchInFens(fens: List[FEN]): Option[FullOpening] =
    fens.foldRight(none[FullOpening]) {
      case (fen, None) => byFen get {
        fen.value.split(' ').take(3) mkString " "
      }
      case (_, found) => found
    }
}
