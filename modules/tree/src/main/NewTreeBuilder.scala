package lila.tree

import chess.format.pgn.{ Comment, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import chess.{ Centis, Color, Ply, Variation }

object NewTreeBuilder:

  import TreeBuilder.{ OpeningOf, LogChessError }

  def apply(
      game: Game,
      analysis: Option[Analysis],
      initialFen: Fen.Full,
      withFlags: ExportOptions,
      logChessError: TreeBuilder.LogChessError
  ): NewRoot =
    val withClocks: Option[Vector[Centis]] = withFlags.clocks.so(game.bothClockStates)
    val drawOfferPlies                     = game.drawOffers.normalizedPlies

    chess.Replay.gameMoveWhileValid(game.sans, initialFen, game.variant) match
      case (init, games, error) =>
        error.foreach: err =>
          logChessError(formatError(game.id, err))

        val openingOf: OpeningOf =
          if withFlags.opening && Variant.list.openingSensibleVariants(game.variant)
          then OpeningDb.findByFullFen
          else _ => None

        val fen                       = Fen.write(init)
        val infos: Vector[Info]       = analysis.so(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.so(_.advices.mapBy(_.ply))

        val metas = Metas(
          ply = init.ply,
          fen = fen,
          check = init.situation.check,
          opening = openingOf(fen),
          clock = withFlags.clocks.so(game.clock.map(c => Centis.ofSeconds(c.limitSeconds.value))),
          crazyData = init.situation.board.crazyData,
          eval = infos.lift(0).map(TreeBuilder.makeEval)
        )

        def makeBranch(g: chess.Game, m: Uci.WithSan, index: Int): NewTree =

          val fen    = Fen.write(g)
          val info   = infos.lift(index - 1)
          val advice = advices.get(g.ply)

          val value = NewBranch(
            id = UciCharPair(m.uci),
            move = m,
            metas = Metas(
              ply = g.ply,
              fen = fen,
              check = g.situation.check,
              opening = openingOf(fen),
              clock = withClocks.flatMap(_.lift((g.ply - init.ply - 1).value)),
              crazyData = g.situation.board.crazyData,
              eval = info.map(TreeBuilder.makeEval),
              glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
              comments = Node.Comments(
                drawOfferPlies(g.ply)
                  .option(TreeBuilder.makeLichessComment(Comment(s"${!g.ply.turn} offers draw")))
                  .toList :::
                  advice
                    .map(_.makeComment(withEval = false, withBestMove = true))
                    .toList
                    .map(TreeBuilder.makeLichessComment)
              )
            )
          )

          val variations = advices
            .get(g.ply)
            .flatMap: adv =>
              games.lift(index - 2).flatMap { (fromGame, _) =>
                withAnalysisChild(
                  game.id,
                  game.variant,
                  Fen.write(fromGame),
                  openingOf,
                  logChessError
                )(adv.info)
              }
            .toList

          chess.Node(value, none, variations)
        end makeBranch

        val tree: Option[NewTree] =
          chess.Tree.build[((chess.Game, Uci.WithSan), Int), NewBranch](
            games.zipWithIndex,
            { case ((game, move), index) => makeBranch(game, move, index + 1) }
          )

        NewRoot(metas, tree)

  private def withAnalysisChild(
      id: GameId,
      variant: Variant,
      fromFen: Fen.Full,
      openingOf: OpeningOf,
      logChessError: LogChessError
  )(info: Info): Option[Variation[NewBranch]] =

    def makeBranch(g: chess.Game, m: Uci.WithSan): NewBranch =
      val fen = Fen.write(g)
      NewBranch(
        id = UciCharPair(m.uci),
        move = m,
        metas = Metas(
          ply = g.ply,
          fen = fen,
          check = g.situation.check,
          opening = openingOf(fen),
          crazyData = g.situation.board.crazyData,
          eval = none
        )
      )

    chess.Replay.gameMoveWhileValid(info.variation.take(20), fromFen, variant) match
      case (_, games, error) =>
        error.foreach: err =>
          logChessError(formatError(id, err))
        chess.Tree
          .build[(chess.Game, Uci.WithSan), NewBranch](games, makeBranch)
          .map(_.updateValue(_.setComp))
          .map(_.toVariation)

  private def formatError(id: GameId, err: chess.ErrorStr) =
    s"TreeBuilder https://lichess.org/$id ${err.value.linesIterator.toList.headOption}"
