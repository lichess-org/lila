package lidraughts.importer

import draughts._

object Chess960 {

  def isStartPosition(board: Board) = board valid true && {

    def rankMatches(f: Option[Piece] => Boolean)(rank: Int) =
      (1 to 8) forall { file => f(board(file, rank)) }

    rankMatches {
      case Some(Piece(White, King)) => true
      case _ => false
    }(1) &&
      rankMatches {
        case Some(Piece(White, Man)) => true
        case _ => false
      }(2) &&
      List(3, 4, 5, 6).forall(rankMatches(_.isEmpty)) &&
      rankMatches {
        case Some(Piece(Black, Man)) => true
        case _ => false
      }(7) &&
      rankMatches {
        case Some(Piece(Black, King)) => true
        case _ => false
      }(8)
  }

  def fixVariantName(v: String) = v.toLowerCase match {
    case "draughts 960" => "chess960"
    case _ => v
  }
}
