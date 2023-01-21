package lila.analyse

import chess.format.pgn.{ Glyphs, Move, Pgn, Tag, Turn, PgnStr }
import chess.opening.*
import chess.{ Color, Status }

import lila.game.GameDrawOffers
import lila.game.Game

final class Annotator(netDomain: lila.common.config.NetDomain):

  def apply(p: Pgn, game: Game, analysis: Option[Analysis]): Pgn =
    annotateStatus(game.winnerColor, game.status) {
      annotateOpening(game.opening) {
        annotateTurns(
          annotateDrawOffers(p, game.drawOffers),
          analysis.??(_.advices)
        )
      }.copy(
        tags = p.tags + Tag(_.Annotator, netDomain)
      )
    }

  def addEvals(p: Pgn, analysis: Analysis): Pgn =
    analysis.infos.foldLeft(p) { (pgn, info) =>
      pgn.updatePly(
        info.ply,
        move => {
          val comment = info.cp
            .map(_.pawns.toString)
            .orElse(info.mate.map(m => s"#${m.value}"))
            .map(c => s"[%eval $c]")
          move.copy(comments = comment.toList ::: move.comments)
        }
      )
    }

  def toPgnString(pgn: Pgn): PgnStr = PgnStr {
    // merge analysis & eval comments
    // 1. e4 { [%eval 0.17] } { [%clk 0:00:30] }
    // 1. e4 { [%eval 0.17] [%clk 0:00:30] }
    s"$pgn\n\n\n".replaceIf("] } { [", "] [")
  }

  private def annotateStatus(winner: Option[Color], status: Status)(p: Pgn) =
    lila.game.StatusText(status, winner, chess.variant.Standard) match
      case ""   => p
      case text => p.updateLastPly(_.copy(result = text.some))

  private def annotateOpening(opening: Option[Opening.AtPly])(p: Pgn) =
    opening.fold(p) { o =>
      p.updatePly(o.ply, _.copy(opening = s"${o.opening.eco} ${o.opening.name}".some))
    }

  private def annotateTurns(p: Pgn, advices: List[Advice]): Pgn =
    advices.foldLeft(p) { (pgn, advice) =>
      pgn.updatePly(
        advice.ply,
        move =>
          move.copy(
            glyphs = Glyphs.fromList(advice.judgment.glyph :: Nil),
            comments = advice.makeComment(withEval = true, withBestMove = true) :: move.comments,
            variations = makeVariation(advice) :: Nil
          )
      )
    }

  private def annotateDrawOffers(pgn: Pgn, drawOffers: GameDrawOffers): Pgn =
    if (drawOffers.isEmpty) pgn
    else
      drawOffers.normalizedPlies.foldLeft(pgn) { (pgn, ply) =>
        pgn.updatePly(ply, move => move.copy(comments = s"${!ply.color} offers draw" :: move.comments))
      }

  private def makeVariation(advice: Advice): List[Turn] =
    Turn.fromMoves(
      advice.info.variation take 20 map { san =>
        Move(san)
      },
      advice.ply
    )
