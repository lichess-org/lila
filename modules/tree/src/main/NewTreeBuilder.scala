package lila.tree

import chess.format.pgn.{ Comment, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import chess.{ Centis, Position, Ply, Variation }

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
    val drawOfferPlies = game.drawOffers.normalizedPlies

    val setup = chess.Position.AndFullMoveNumber(game.variant, initialFen)
    val openingOf: OpeningOf =
      if withFlags.opening && Variant.list.openingSensibleVariants(game.variant)
      then OpeningDb.findByFullFen
      else _ => None

    val fen = Fen.write(setup)
    val infos: Vector[Info] = analysis.so(_.infos.toVector)
    val advices: Map[Ply, Advice] = analysis.so(_.advices.mapBy(_.ply))

    val metas = Metas(
      ply = setup.ply,
      fen = fen,
      check = setup.position.check,
      opening = openingOf(fen),
      clock = withFlags.clocks.so(
        game.clock.map(c => Centis.ofSeconds(c.limitSeconds.value)).map(Clock(_))
      ),
      crazyData = setup.position.crazyData,
      eval = infos.lift(0).map(TreeBuilder.makeEval)
    )

    def makeBranch(move: chess.MoveOrDrop, ply: Ply): NewTree =

      val fen = Fen.write(move.after, ply.fullMoveNumber)
      val index = (ply - setup.ply - 1).value
      val info = infos.lift(index)
      val advice = advices.get(ply)

      val value = NewBranch(
        id = UciCharPair(move.toUci),
        move = Uci.WithSan(move.toUci, move.toSanStr),
        metas = Metas(
          ply = ply,
          fen = fen,
          check = move.after.position.check,
          opening = openingOf(fen),
          clock = withClocks.flatMap(_.lift(index)).map(Clock(_)),
          crazyData = move.after.position.crazyData,
          eval = info.map(TreeBuilder.makeEval),
          glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
          comments = Node.Comments(
            drawOfferPlies(ply)
              .option(TreeBuilder.makeLichessComment(Comment(s"${!ply.turn} offers draw")))
              .toList :::
              advice
                .map(_.makeComment(withEval = false, withBestMove = true))
                .toList
                .map(TreeBuilder.makeLichessComment)
          )
        )
      )

      val variations = advices
        .get(ply)
        .flatMap: adv =>
          withAnalysisChild(game.id, move.before, ply - 1, openingOf, logChessError)(adv.info)
        .toList

      chess.Node(value, none, variations)

    val (tree, error) =
      setup.position.buildTree(game.sans, setup.ply)(step => makeBranch(step.move, step.ply))

    error.foreach(e => logChessError(formatError(game.id, e)))
    NewRoot(metas, tree)

  private def withAnalysisChild(
      id: GameId,
      position: Position,
      ply: Ply,
      openingOf: OpeningOf,
      logChessError: LogChessError
  )(info: Info): Option[Variation[NewBranch]] =

    def makeBranch(move: chess.MoveOrDrop, ply: Ply): NewTree =
      val fen = Fen.write(move.after, ply.fullMoveNumber)
      chess.Node(
        NewBranch(
          id = UciCharPair(move.toUci),
          move = Uci.WithSan(move.toUci, move.toSanStr),
          metas = Metas(
            ply = ply,
            fen = fen,
            check = move.after.check,
            opening = openingOf(fen),
            crazyData = move.after.position.crazyData,
            eval = none
          )
        )
      )

    val (tree, error) =
      position.buildTree(info.variation.take(20), ply)(step => makeBranch(step.move, step.ply))
    error.foreach(e => logChessError(formatError(id, e)))
    tree.map(_.updateValue(_.setComp).toVariation)

  private def formatError(id: GameId, err: chess.ErrorStr) =
    s"TreeBuilder https://lichess.org/$id $err"
