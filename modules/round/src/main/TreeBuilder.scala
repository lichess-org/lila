package lila.round

import chess.format.pgn.Glyphs
import chess.format.{ Forsyth, Uci, UciCharPair }
import chess.opening._
import chess.variant.Variant
import lila.analyse.{ Analysis, Info, Advice }
import lila.socket.tree._

import play.api.libs.json._

object TreeBuilder {

  private type Ply = Int
  private type OpeningOf = String => Option[FullOpening]

  private def makeEval(info: Info) = Node.Eval(
    cp = info.score.map(_.ceiled.centipawns),
    mate = info.mate,
    best = info.best)

  def apply(
    game: lila.game.Game,
    analysis: Option[Analysis],
    initialFen: String,
    withOpening: Boolean): Root = apply(
    id = game.id,
    pgnMoves = game.pgnMoves,
    variant = game.variant,
    analysis = analysis,
    initialFen = initialFen,
    withOpening = withOpening)

  def apply(
    id: String,
    pgnMoves: List[String],
    variant: Variant,
    analysis: Option[Analysis],
    initialFen: String,
    withOpening: Boolean): Root = {
    chess.Replay.gameMoveWhileValid(pgnMoves, initialFen, variant) match {
      case (init, games, error) =>
        error foreach logChessError(id)
        val openingOf: OpeningOf =
          if (withOpening && Variant.openingSensibleVariants(variant)) FullOpeningDB.findByFen
          else _ => None
        val fen = Forsyth >> init
        val infos: Vector[Info] = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(_.advices.map { a =>
          a.ply -> a
        }.toMap)
        val root = Root(
          ply = init.turns,
          fen = fen,
          check = init.situation.check,
          opening = openingOf(fen),
          crazyData = init.situation.board.crazyData,
          eval = infos lift 0 map makeEval)
        def makeBranch(index: Int, g: chess.Game, m: Uci.WithSan) = {
          val fen = Forsyth >> g
          val info = infos lift (index - 1)
          val advice = advices get g.turns
          val branch = Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            check = g.situation.check,
            opening = openingOf(fen),
            crazyData = g.situation.board.crazyData,
            eval = info map makeEval,
            glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
            comments = Node.Comments {
              advice.map(_.makeComment(false, true)).toList.map { text =>
                Node.Comment(
                  Node.Comment.Id.make,
                  Node.Comment.Text(text),
                  Node.Comment.Author.Lichess)
              }
            })
          advices.get(g.turns + 1).flatMap { adv =>
            games.lift(index - 1).map {
              case (fromGame, _) =>
                val fromFen = Forsyth >> fromGame
                withAnalysisChild(id, branch, variant, fromFen, openingOf)(adv.info)
            }
          } getOrElse branch
        }
        games.zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest => root prependChild rest.foldLeft(makeBranch(i + 1, g, m)) {
            case (node, ((g, m), i)) => makeBranch(i + 1, g, m) prependChild node
          }
        }
    }
  }

  private def withAnalysisChild(id: String, root: Branch, variant: Variant, fromFen: String, openingOf: OpeningOf)(info: Info): Branch = {
    def makeBranch(index: Int, g: chess.Game, m: Uci.WithSan) = {
      val fen = Forsyth >> g
      Branch(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = fen,
        check = g.situation.check,
        opening = openingOf(fen),
        crazyData = g.situation.board.crazyData,
        eval = none)
    }
    chess.Replay.gameMoveWhileValid(info.variation take 20, fromFen, variant) match {
      case (init, games, error) =>
        error foreach logChessError(id)
        games.zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest => root addChild rest.foldLeft(makeBranch(i + 1, g, m)) {
            case (node, ((g, m), i)) => makeBranch(i + 1, g, m) addChild node
          }
        }
    }
  }

  private val logChessError = (id: String) => (err: String) =>
    logger.warn(s"round.TreeBuilder http://lichess.org/$id ${err.lines.toList.headOption}")
}
