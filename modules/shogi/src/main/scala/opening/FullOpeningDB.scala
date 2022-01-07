package shogi
package opening

import format.FEN
import cats.syntax.option._

object FullOpeningDB {

  private def all: Vector[FullOpening] =
    FullOpeningPartA.db

  private lazy val byFen: collection.Map[String, FullOpening] =
    all
      .map { o =>
        o.fen -> o
      }
      .to(Map)

  def findByFen(fen: FEN): Option[FullOpening] =
    fen.value.split(' ').take(3) match {
      case Array(boardPocket, turn, pocket) =>
        val board =
          if (boardPocket.count('/' ==) == 9) boardPocket.split('/').take(9).mkString("/")
          else boardPocket
        byFen get List(board, turn, pocket).mkString(" ")
      case _ => None
    }

  val SEARCH_MAX_PLIES = 40

  // assumes standard initial FEN and variant
  def search(usis: Seq[shogi.format.usi.Usi]): Option[FullOpening.AtPly] =
    shogi.Replay
      .situations(
        usis.take(SEARCH_MAX_PLIES),
        None,
        variant.Standard
      )
      .toOption
      .flatMap {
        _.zipWithIndex.tail.foldRight(none[FullOpening.AtPly]) {
          case ((situation, ply), None) =>
            // val color = if (ply % 2 == 0) " b " else " w "
            val fen = format.Forsyth.exportSituation(situation)
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
