package lila.opening

import cats.syntax.all.*
import chess.format.pgn.{ Pgn, SanStr }
import chess.format.{ Fen, OpeningFen, Uci }
import chess.opening.{ Opening, OpeningDb, OpeningKey, OpeningName }

import lila.game.Game

case class OpeningPage(
    query: OpeningQuery,
    explored: Option[OpeningExplored],
    wiki: Option[OpeningWiki]
):
  export query.{ closestOpening, exactOpening, name, openingAndExtraMoves }

  def nameParts: NamePart.NamePartList = openingAndExtraMoves match
    case (op, moves) => (op so NamePart.from) ::: NamePart.from(moves)

case object NamePart:
  type NamePartList = List[Either[SanStr, (NameSection, Option[OpeningKey])]]
  def from(op: Opening): NamePartList =
    val sections = NameSection.sectionsOf(op.name)
    sections.toList.mapWithIndex: (name, i) =>
      Right(
        name ->
          OpeningDb.shortestLines
            .get(OpeningKey.fromName(OpeningName(sections.take(i + 1).mkString("_"))))
            .map(_.key)
      )
  def from(moves: List[SanStr]): NamePartList = moves.map(Left.apply)

case class ResultCounts(
    white: Long,
    draws: Long,
    black: Long
):
  lazy val sum: Long = white + draws + black

  def whitePercent                      = percentOf(white)
  def drawsPercent                      = percentOf(draws)
  def blackPercent                      = percentOf(black)
  private def percentOf(v: Long): Float = (v.toFloat * 100 / sum)

case class OpeningNext(
    san: SanStr,
    uci: Uci.Move,
    fen: OpeningFen,
    query: OpeningQuery,
    result: ResultCounts,
    percent: Double,
    opening: Option[Opening],
    shortName: Option[NameSection]
)

case class GameWithPgn(game: Game, pgn: Pgn)

case class OpeningExplored(
    result: ResultCounts,
    games: List[GameWithPgn],
    next: List[OpeningNext],
    history: PopularityHistoryPercent
) {
  def lastPopularityPercent: Option[Float] = history.lastOption
}

object OpeningPage:
  def apply(
      query: OpeningQuery,
      exploredPosition: Option[OpeningExplorer.Position],
      games: List[GameWithPgn],
      history: PopularityHistoryPercent,
      wiki: Option[OpeningWiki]
  ): OpeningPage =
    OpeningPage(
      query = query,
      exploredPosition map { exp =>
        OpeningExplored(
          result = ResultCounts(exp.white, exp.draws, exp.black),
          games = games,
          next = exp.moves
            .flatMap { m =>
              for {
                uci  <- Uci.Move(m.uci)
                move <- query.position.move(uci).toOption
                result  = ResultCounts(m.white, m.draws, m.black)
                fen     = Fen writeOpening move.situationAfter
                opening = OpeningDb findByOpeningFen fen
              } yield OpeningNext(
                m.san,
                uci,
                fen,
                query.copy(replay = query.replay addMove move),
                result,
                (result.sum * 100d / exp.movesSum),
                opening,
                shortName = NameSection.variationName(query.exactOpening, opening)
              )
            }
            .sortBy(-_.result.sum),
          history = history
        )
      },
      wiki
    )
