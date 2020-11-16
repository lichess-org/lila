package chess
package opening

import format.FEN

object FullOpeningDB {

  private def all: Vector[FullOpening] =
    FullOpeningPartA.db ++ FullOpeningPartB.db ++ FullOpeningPartC.db ++ FullOpeningPartD.db ++ FullOpeningPartE.db

  private lazy val byFen: collection.Map[String, FullOpening] =
    all
      .map { o =>
        o.fen -> o
      }
      .to(Map)

  def findByFen(fen: FEN): Option[FullOpening] =
    fen.value.split(' ').take(4) match {
      case Array(boardPocket, turn, castle, ep) =>
        val board =
          if (boardPocket.contains('[')) boardPocket.takeWhile('[' !=)
          else if (boardPocket.count('/' ==) == 8) boardPocket.split('/').take(8).mkString("/")
          else boardPocket
        byFen get List(board, turn, castle, ep).mkString(" ")
      case _ => None
    }

  val SEARCH_MAX_PLIES = 40

  // assumes standard initial FEN and variant
  def search(moveStrs: Iterable[String]): Option[FullOpening.AtPly] =
    chess.Replay
      .situations(
        moveStrs.take(SEARCH_MAX_PLIES).takeWhile(san => !san.contains('*')),
        None,
        variant.Standard
      )
      .toOption
      .flatMap {
        _.zipWithIndex.drop(1).foldRight(none[FullOpening.AtPly]) {
          case ((situation, ply), None) =>
            val color = if (ply % 2 == 0) " w " else " b "
            val fen = format.Forsyth.exportBoard(situation.board) +
            color + format.Forsyth.exportCrazyPocket(situation.board) + ply
            byFen get fen map (_ atPly ply)
          case (_, found) => found
        }
      }

  def searchInFens(fens: List[FEN]): Option[FullOpening] =
    fens.foldRight(none[FullOpening]) {
      case (fen, None) => findByFen(fen)
      case (_, found)  => found
    }
}
