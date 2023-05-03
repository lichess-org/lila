package lila.round

import chess.{ Centis, Color, Ply, Node as ChessNode, Variation }
import chess.format.pgn.{ Comment, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.opening.*
import chess.variant.Variant
import JsonView.WithFlags
import lila.analyse.{ Advice, Analysis, Info }
import lila.tree.{ NewBranch, NewRoot, NewTree, Eval, Metas }
import lila.tree.Node
import chess.Tree

object TreeBuilder:

  private type OpeningOf = Fen.Epd => Option[Opening]

  private def makeEval(info: Info) = Eval(cp = info.cp, mate = info.mate, best = info.best)

  def apply(
      game: lila.game.Game,
      analysis: Option[Analysis],
      initialFen: Fen.Epd,
      withFlags: WithFlags
  ): NewRoot =
    val withClocks: Option[Vector[Centis]] = withFlags.clocks ?? game.bothClockStates
    val drawOfferPlies                     = game.drawOffers.normalizedPlies
    chess.Replay.gameMoveWhileValid(game.sans, initialFen, game.variant) match
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val openingOf: OpeningOf =
          if (withFlags.opening && Variant.list.openingSensibleVariants(game.variant)) OpeningDb.findByEpdFen
          else _ => None
        val fen                       = Fen write init
        val infos: Vector[Info]       = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(_.advices.mapBy(_.ply))
        val metas = Metas(
          ply = init.ply,
          fen = fen,
          check = init.situation.check,
          opening = openingOf(fen),
          clock = withFlags.clocks ?? game.clock.map { c =>
            Centis.ofSeconds(c.limitSeconds.value)
          },
          crazyData = init.situation.board.crazyData,
          eval = infos lift 0 map makeEval
        )
        val root = NewRoot(metas, None)
        def makeNode(index: Int, g: chess.Game, m: Uci.WithSan) =
          val fen    = Fen write g
          val info   = infos lift (index - 1)
          val advice = advices get g.ply
          val branch = NewBranch(
            id = UciCharPair(m.uci),
            path = UciPath.root, // TODO: fix or remove path
            move = m,
            metas = Metas(
              ply = g.ply,
              fen = fen,
              check = g.situation.check,
              opening = openingOf(fen),
              clock = withClocks.flatMap(_.lift((g.ply - init.ply - 1).value)),
              crazyData = g.situation.board.crazyData,
              eval = info map makeEval,
              glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
              comments = Node.Comments {
                drawOfferPlies(g.ply)
                  .option(makeLichessComment(Comment(s"${!g.ply.color} offers draw")))
                  .toList :::
                  advice
                    .map(_.makeComment(withEval = false, withBestMove = true))
                    .toList
                    .map(makeLichessComment)
              }
            )
          )

          val node = ChessNode(branch, None, Nil)
          advices.get(g.ply + 1).flatMap { adv =>
            games.lift(index - 1).map { case (fromGame, _) =>
              withAnalysisChild(game.id, node, game.variant, Fen write fromGame, openingOf)(adv.info)
            }
          } getOrElse node
        val tree = ChessNode.buildWithNode(games.zipWithIndex, { case ((g, m), i) => makeNode(i + 1, g, m) })
        root.withTree(tree)

  private def makeLichessComment(c: Comment) =
    Node.Comment(
      Node.Comment.Id.make,
      c into Node.Comment.Text,
      Node.Comment.Author.Lichess
    )

  private def withAnalysisChild(
      id: GameId,
      root: NewTree,
      variant: Variant,
      fromFen: Fen.Epd,
      openingOf: OpeningOf
  )(info: Info): NewTree =
    def makeBranch(g: chess.Game, m: Uci.WithSan): NewBranch =
      val fen = Fen write g
      val metas = Metas(
        ply = g.ply,
        fen = fen,
        check = g.situation.check,
        opening = openingOf(fen),
        crazyData = g.situation.board.crazyData,
        eval = none
      )
      NewBranch(
        id = UciCharPair(m.uci),
        path = UciPath.root, // TODO: fix or remove path
        move = m,
        metas = metas
      )
    chess.Replay.gameMoveWhileValid(info.variation take 20, fromFen, variant) match
      case (_, games, error) =>
        error foreach logChessError(id)
        Variation.build(games, makeBranch).fold(root)(root.addVariation)

  private val logChessError = (id: GameId) =>
    (err: chess.ErrorStr) =>
      logger.warn(s"round.TreeBuilder https://lichess.org/$id ${err.value.linesIterator.toList.headOption}")
