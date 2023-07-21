package lila.importer

import chess.*

private object Chess960:

  def isStartPosition(sit: Situation) =
    val strict =
      def rankMatches(f: Option[Piece] => Boolean)(rank: Rank) =
        File.all.forall: file =>
          f(sit.board(file, rank))
      rankMatches {
        case Some(Piece(White, King | Queen | Rook | Knight | Bishop)) => true
        case _                                                         => false
      }(Rank.First) &&
      rankMatches {
        case Some(Piece(White, Pawn)) => true
        case _                        => false
      }(Rank.Second) &&
      List(Rank.Third, Rank.Fourth, Rank.Fifth, Rank.Sixth).forall(rankMatches(_.isEmpty)) &&
      rankMatches {
        case Some(Piece(Black, Pawn)) => true
        case _                        => false
      }(Rank.Seventh) &&
      rankMatches {
        case Some(Piece(Black, King | Queen | Rook | Knight | Bishop)) => true
        case _                                                         => false
      }(Rank.Eighth)

    variant.Chess960.valid(sit, strict)
