package lila.tutor

import lila.game.Game
import chess.Color
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.format.{ FEN, Forsyth }

case class TutorOpeningReport(opening: String, games: NbGames, moves: NbMoves)

object TutorOpeningReport {

  type OpeningMap    = Color.Map[ColorOpenings]
  type ColorOpenings = List[(String, TutorOpeningReport)]

  object builder {
    case class Opening(opening: FullOpening, games: NbGames, moves: NbMoves)
    type OpeningMap = Map[FEN, Opening]
  }

  def aggregate(openings: OpeningMap, richPov: RichPov) = {

    import richPov._

    val opening = pov.game.variant.standard ??
      replay
        .map(s => FEN(Forsyth exportStandardPositionTurnCastlingEp s))
        .zipWithIndex
        .drop(1)
        .foldRight(none[FullOpening.AtPly]) {
          case ((fen, ply), None) => FullOpeningDB.findByFen(fen).map(_ atPly ply)
          case (_, found)         => found
        }

    opening.fold(openings) { op =>
      openings.update(
        pov.color,
        _.updatedWith(FEN(op.opening.fen)) { opt =>
          val prev = opt | TutorOpeningReport(op.opening, op.ply, NbGames(0), NbMoves(0))
          prev
            .copy(
              ply = prev.ply atMost op.ply,
              games = prev.games + 1,
              moves = prev.moves + op.ply / 2
            )
            .some
        }
      )
    }
  }

  def postProcess(openings: ColorOpeningMap): ColorOpeningMap = {

    // val games = openings.values.map(_.games.value).sum

    def familyOf(op: FullOpening) = op.name.takeWhile(_ != ':')
    val families: List[(String, List[TutorOpeningReport])] =
      openings.values
        .groupBy(op => familyOf(op.opening))
        .map(x => x._1 -> x._2.toList)
        .toList
        .sortBy(-_._2.size)
        .take(5)

    // def variationOf(op: FullOpening) = op.name.takeWhile(_ != ',')
    // val variations: List[(String, Int)] = openings.values
    //   .foldLeft(Map.empty[String, Int]) { case (vars, op) =>
    //     vars.updatedWith(variationOf(op.opening))(nb => Some(~nb + 1))
    //   }
    //   .toList
    //   .sortBy(-_._2)
    // .takeWhile(_._2 >= threshold)

    // val familySet = families.map(_._1).toSet

    families.flatMap { case (_, reports) =>
      reports.sortBy(_.ply).headOption map { report =>
        val opening = report.opening
        FEN(opening.fen) -> TutorOpeningReport(
          opening = opening,
          ply = report.ply,
          games = NbGames(reports.map(_.games.value).sum),
          moves = NbMoves(reports.map(_.moves.value).sum)
        )
      }
    }.toMap
  }
}
