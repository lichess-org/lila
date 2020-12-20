package lila.round

import chess.Centis
import chess.format.pgn.Glyphs
import chess.format.{ FEN, Forsyth, Uci, UciCharPair }
import chess.opening._
import chess.variant.Variant
import JsonView.WithFlags
import lila.analyse.{ Advice, Analysis, Info }
import lila.tree._

object TreeBuilder {

  private type Ply       = Int
  private type OpeningOf = FEN => Option[FullOpening]

  private def makeEval(info: Info) =
    Eval(
      cp = info.cp,
      mate = info.mate,
      best = info.best
    )

  def apply(
      game: lila.game.Game,
      analysis: Option[Analysis],
      initialFen: FEN,
      withFlags: WithFlags
  ): Root =
    apply(
      id = game.id,
      pgnMoves = game.pgnMoves,
      variant = game.variant,
      analysis = analysis,
      initialFen = initialFen,
      withFlags = withFlags,
      clocks = withFlags.clocks ?? game.bothClockStates
    )

  def apply(
      id: String,
      pgnMoves: Vector[String],
      variant: Variant,
      analysis: Option[Analysis],
      initialFen: FEN,
      withFlags: WithFlags,
      clocks: Option[Vector[Centis]]
  ): Root = {
    val withClocks: Option[Vector[Centis]] = withFlags.clocks ?? clocks
    chess.Replay.gameMoveWhileValid(pgnMoves, initialFen.value, variant) match {
      case (init, games, error) =>
        error foreach logChessError(id)
        val openingOf: OpeningOf =
          if (withFlags.opening && Variant.openingSensibleVariants(variant)) FullOpeningDB.findByFen
          else _ => None
        val fen                 = Forsyth >> init
        val infos: Vector[Info] = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(_.advices.view.map { a =>
          a.ply -> a
        }.toMap)
        val root = Root(
          ply = init.turns,
          fen = fen,
          check = init.situation.check,
          opening = openingOf(FEN(fen)),
          clock = withClocks.flatMap(_.headOption),
          crazyData = init.situation.board.crazyData,
          eval = infos lift 0 map makeEval
        )
        def makeBranch(index: Int, g: chess.Game, m: Uci.WithSan) = {
          val fen    = Forsyth >> g
          val info   = infos lift (index - 1)
          val advice = advices get g.turns
          val branch = Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            check = g.situation.check,
            opening = openingOf(FEN(fen)),
            clock = withClocks flatMap (_ lift (g.turns - init.turns - 1)),
            crazyData = g.situation.board.crazyData,
            eval = info map makeEval,
            glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
            comments = Node.Comments {
              advice.map(_.makeComment(withEval = false, withBestMove = true)).toList.map { text =>
                Node.Comment(
                  Node.Comment.Id.make,
                  Node.Comment.Text(text),
                  Node.Comment.Author.Lichess
                )
              }
            }
          )
          advices.get(g.turns + 1).flatMap { adv =>
            games.lift(index - 1).map {
              case (fromGame, _) =>
                val fromFen = FEN(Forsyth >> fromGame)
                withAnalysisChild(id, branch, variant, fromFen, openingOf)(adv.info)
            }
          } getOrElse branch
        }
        games.zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest =>
            root prependChild rest.foldLeft(makeBranch(i + 1, g, m)) {
              case (node, ((g, m), i)) => makeBranch(i + 1, g, m) prependChild node
            }
        }
    }
  }

  private def withAnalysisChild(
      id: String,
      root: Branch,
      variant: Variant,
      fromFen: FEN,
      openingOf: OpeningOf
  )(info: Info): Branch = {
    def makeBranch(g: chess.Game, m: Uci.WithSan) = {
      val fen = Forsyth >> g
      Branch(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = fen,
        check = g.situation.check,
        opening = openingOf(FEN(fen)),
        crazyData = g.situation.board.crazyData,
        eval = none
      )
    }
    chess.Replay.gameMoveWhileValid(info.variation take 20, fromFen.value, variant) match {
      case (_, games, error) =>
        error foreach logChessError(id)
        games.reverse match {
          case Nil => root
          case (g, m) :: rest =>
            root addChild rest
              .foldLeft(makeBranch(g, m)) {
                case (node, (g, m)) => makeBranch(g, m) addChild node
              }
              .setComp
        }
    }
  }

  private val logChessError = (id: String) =>
    (err: String) =>
      logger.warn(s"round.TreeBuilder https://lichess.org/$id ${err.linesIterator.toList.headOption}")
}
