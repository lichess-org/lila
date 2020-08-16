package lila.importer

import chess._

private object Chess960 {

  def isStartPosition(board: Board) =
    board valid {

      def rankMatches(f: Option[Piece] => Boolean)(rank: Int) =
        (1 to 8) forall { file =>
          f(board(file, rank))
        }

      rankMatches {
        case Some(Piece(White, King | Queen | Rook | Knight | Bishop)) => true
        case _                                                         => false
      }(1) &&
      rankMatches {
        case Some(Piece(White, Pawn)) => true
        case _                        => false
      }(2) &&
      List(3, 4, 5, 6).forall(rankMatches(_.isEmpty)) &&
      rankMatches {
        case Some(Piece(Black, Pawn)) => true
        case _                        => false
      }(7) &&
      rankMatches {
        case Some(Piece(Black, King | Queen | Rook | Knight | Bishop)) => true
        case _                                                         => false
      }(8)
    }

  def fixVariantName(v: String) =
    v.toLowerCase match {
      case "chess 960"   => "chess960"
      case "fisherandom" => "chess960" // I swear, sometimes...
      case _             => v
    }
}
