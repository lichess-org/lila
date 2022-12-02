package lila.round

import chess.{ Centis, Color }
import chess.format.pgn.Glyphs
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import JsonView.WithFlags
import lila.analyse.{ Advice, Analysis, Info }
import lila.tree.*

object TreeBuilder:

  private type Ply       = Int
  private type OpeningOf = Fen => Option[Opening]

  private def makeEval(info: Info) =
    Eval(
      cp = info.cp,
      mate = info.mate,
      best = info.best
    )

  def apply(
      game: lila.game.Game,
      analysis: Option[Analysis],
      initialFen: Fen,
      withFlags: WithFlags
  ): Root =
    val withClocks: Option[Vector[Centis]] = withFlags.clocks ?? game.bothClockStates
    val drawOfferPlies                     = game.drawOffers.normalizedPlies
    chess.Replay.gameMoveWhileValid(game.pgnMoves, initialFen, game.variant) match
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val openingOf: OpeningOf =
          if (withFlags.opening && Variant.openingSensibleVariants(game.variant))
            fen => OpeningDb.findByFen(fen.opening)
          else _ => None
        val fen                 = Fen write init
        val infos: Vector[Info] = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(
          _.advices.view
            .map { a =>
              a.ply -> a
            }
            .toMap
        )
        val root = Root(
          ply = init.turns,
          fen = fen,
          check = init.situation.check,
          opening = openingOf(fen),
          clock = withFlags.clocks ?? game.clock.map { c =>
            Centis.ofSeconds(c.limitSeconds)
          },
          crazyData = init.situation.board.crazyData,
          eval = infos lift 0 map makeEval
        )
        def makeBranch(index: Int, g: chess.Game, m: Uci.WithSan) =
          val fen    = Fen write g
          val info   = infos lift (index - 1)
          val advice = advices get g.turns
          val branch = Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            check = g.situation.check,
            opening = openingOf(fen),
            clock = withClocks flatMap (_ lift (g.turns - init.turns - 1)),
            crazyData = g.situation.board.crazyData,
            eval = info map makeEval,
            glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
            comments = Node.Comments {
              drawOfferPlies(g.turns)
                .option(makeLichessComment(s"${!Color.fromPly(g.turns)} offers draw"))
                .toList :::
                advice
                  .map(_.makeComment(withEval = false, withBestMove = true))
                  .toList
                  .map(makeLichessComment)
            }
          )
          advices.get(g.turns + 1).flatMap { adv =>
            games.lift(index - 1).map { case (fromGame, _) =>
              withAnalysisChild(game.id, branch, game.variant, Fen write fromGame, openingOf)(adv.info)
            }
          } getOrElse branch
        games.zipWithIndex.reverse match
          case Nil => root
          case ((g, m), i) :: rest =>
            root prependChild rest.foldLeft(makeBranch(i + 1, g, m)) { case (node, ((g, m), i)) =>
              makeBranch(i + 1, g, m) prependChild node
            }

  private def makeLichessComment(text: String) =
    Node.Comment(
      Node.Comment.Id.make,
      Node.Comment.Text(text),
      Node.Comment.Author.Lichess
    )

  private def withAnalysisChild(
      id: GameId,
      root: Branch,
      variant: Variant,
      fromFen: Fen,
      openingOf: OpeningOf
  )(info: Info): Branch =
    def makeBranch(g: chess.Game, m: Uci.WithSan) =
      val fen = Fen write g
      Branch(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = fen,
        check = g.situation.check,
        opening = openingOf(fen),
        crazyData = g.situation.board.crazyData,
        eval = none
      )
    chess.Replay.gameMoveWhileValid(info.variation take 20, fromFen, variant) match
      case (_, games, error) =>
        error foreach logChessError(id)
        games.reverse match
          case Nil => root
          case (g, m) :: rest =>
            root addChild rest
              .foldLeft(makeBranch(g, m)) { case (node, (g, m)) =>
                makeBranch(g, m) addChild node
              }
              .setComp

  private val logChessError = (id: GameId) =>
    (err: String) =>
      logger.warn(s"round.TreeBuilder https://lichess.org/$id ${err.linesIterator.toList.headOption}")
