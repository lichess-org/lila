package lila.round

import chess.{ Centis, Color, Ply }
import chess.format.pgn.{ Comment, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import JsonView.WithFlags
import lila.analyse.{ Advice, Analysis, Info }
import lila.tree.*

object TreeBuilder:

  private type OpeningOf = Fen.Epd => Option[Opening]

  private def makeEval(info: Info) = Eval(cp = info.cp, mate = info.mate, best = info.best)

  def apply(
      game: lila.game.Game,
      analysis: Option[Analysis],
      initialFen: Fen.Epd,
      withFlags: WithFlags
  ): Root =
    val withClocks: Option[Vector[Centis]] = withFlags.clocks so game.bothClockStates
    val drawOfferPlies                     = game.drawOffers.normalizedPlies
    chess.Replay.gameMoveWhileValid(game.sans, initialFen, game.variant) match
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val openingOf: OpeningOf =
          if withFlags.opening && Variant.list.openingSensibleVariants(game.variant) then
            OpeningDb.findByEpdFen
          else _ => None
        val fen                       = Fen write init
        val infos: Vector[Info]       = analysis.so(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.so(_.advices.mapBy(_.ply))
        val root = Root(
          ply = init.ply,
          fen = fen,
          check = init.situation.check,
          opening = openingOf(fen),
          clock = withFlags.clocks so game.clock.map { c =>
            Centis.ofSeconds(c.limitSeconds.value)
          },
          crazyData = init.situation.board.crazyData,
          eval = infos lift 0 map makeEval
        )
        def makeBranch(index: Int, g: chess.Game, m: Uci.WithSan) =
          val fen    = Fen write g
          val info   = infos lift (index - 1)
          val advice = advices get g.ply
          val branch = Branch(
            id = UciCharPair(m.uci),
            ply = g.ply,
            move = m,
            fen = fen,
            check = g.situation.check,
            opening = openingOf(fen),
            clock = withClocks.flatMap(_.lift((g.ply - init.ply - 1).value)),
            crazyData = g.situation.board.crazyData,
            eval = info map makeEval,
            glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
            comments = Node.Comments {
              drawOfferPlies(g.ply)
                .option(makeLichessComment(Comment(s"${!g.ply.turn} offers draw")))
                .toList :::
                advice
                  .map(_.makeComment(withEval = false, withBestMove = true))
                  .toList
                  .map(makeLichessComment)
            }
          )
          advices.get(g.ply + 1).flatMap { adv =>
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

  private def makeLichessComment(c: Comment) =
    Node.Comment(
      Node.Comment.Id.make,
      c into Node.Comment.Text,
      Node.Comment.Author.Lichess
    )

  private def withAnalysisChild(
      id: GameId,
      root: Branch,
      variant: Variant,
      fromFen: Fen.Epd,
      openingOf: OpeningOf
  )(info: Info): Branch =
    def makeBranch(g: chess.Game, m: Uci.WithSan) =
      val fen = Fen write g
      Branch(
        id = UciCharPair(m.uci),
        ply = g.ply,
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
    (err: chess.ErrorStr) =>
      logger.warn(s"round.TreeBuilder https://lichess.org/$id ${err.value.linesIterator.toList.headOption}")
