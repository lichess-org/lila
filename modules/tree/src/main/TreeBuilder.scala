package lila.tree

import chess.format.pgn.{ Comment, Glyphs }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.opening.*
import chess.variant.Variant
import chess.{ Centis, Ply, Position }

object TreeBuilder:

  type LogChessError           = String => Unit
  private[tree] type OpeningOf = Fen.Full => Option[Opening]

  private[tree] def makeEval(info: Info) = Eval(cp = info.cp, mate = info.mate, best = info.best)

  def apply(
      game: Game,
      analysis: Option[Analysis],
      initialFen: Fen.Full,
      withFlags: ExportOptions,
      logChessError: LogChessError
  ): Root =
    val withClocks: Option[Vector[Centis]] = withFlags.clocks.so(game.bothClockStates)
    val drawOfferPlies                     = game.drawOffers.normalizedPlies
    val init                               = chess.Position.AndFullMoveNumber(game.variant, initialFen.some)
    val fen                                = Fen.write(init)
    val infos: Vector[Info]                = analysis.so(_.infos.toVector)
    val openingOf: OpeningOf               =
      if withFlags.opening && Variant.list.openingSensibleVariants(game.variant)
      then OpeningDb.findByFullFen
      else _ => None
    val advices: Map[Ply, Advice] = analysis.so(_.advices.mapBy(_.ply))

    val root = Root(
      ply = init.ply,
      fen = fen,
      check = init.position.check,
      opening = openingOf(fen),
      clock = withFlags.clocks.so:
        game.clock.map(c => Centis.ofSeconds(c.limitSeconds.value)).map(Clock(_))
      ,
      crazyData = init.position.crazyData,
      eval = infos.lift(0).map(makeEval)
    )

    init.position
      .play(game.sans, init.ply)(step => chess.MoveOrDrop.WithPly(step.move, step.ply))
      .fold(
        error =>
          logChessError(formatError(game.id, error))
          root
        ,
        games =>
          def makeBranch(index: Int, move: chess.MoveOrDrop.WithPly): Branch =
            val fen    = Fen.write(move.after, move.ply.fullMoveNumber)
            val info   = infos.lift(index - 1)
            val advice = advices.get(move.ply)

            val branch = Branch(
              id = UciCharPair(move.toUci),
              ply = move.ply,
              move = Uci.WithSan(move.toUci, move.toSanStr),
              fen = fen,
              check = move.after.check,
              opening = openingOf(fen),
              clock = withClocks.flatMap(_.lift((move.ply - init.ply - 1).value)).map(Clock(_)),
              crazyData = move.after.crazyData,
              eval = info.map(makeEval),
              glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
              comments = Node.Comments(
                drawOfferPlies(move.ply)
                  .option(makeLichessComment(Comment(s"${!move.ply.turn} offers draw")))
                  .toList :::
                  advice
                    .map(_.makeComment(withEval = false, withBestMove = true))
                    .toList
                    .map(makeLichessComment)
              )
            )

            advices
              .get(move.ply + 1)
              .map { adv =>
                withAnalysisChild(
                  game.id,
                  branch,
                  move.after,
                  move.ply,
                  openingOf,
                  logChessError
                )(adv.info)
              }
              .getOrElse(branch)

          games.zipWithIndex.reverse match
            case Nil            => root
            case (g, i) :: rest =>
              root.prependChild(rest.foldLeft(makeBranch(i + 1, g)) { case (node, (g, i)) =>
                makeBranch(i + 1, g).prependChild(node)
              })
      )

  private[tree] def makeLichessComment(c: Comment) =
    Node.Comment(
      Node.Comment.Id.make,
      c.into(Node.Comment.Text),
      Node.Comment.Author.Lichess
    )

  private def withAnalysisChild(
      id: GameId,
      root: Branch,
      position: Position,
      ply: Ply,
      openingOf: OpeningOf,
      logChessError: LogChessError
  )(info: Info): Branch =

    def makeBranch(m: chess.MoveOrDrop, ply: Ply): Branch =
      val fen = Fen.write(m.after, ply.fullMoveNumber)
      Branch(
        id = UciCharPair(m.toUci),
        ply = ply,
        move = Uci.WithSan(m.toUci, m.toSanStr),
        fen = fen,
        check = m.after.position.check,
        opening = openingOf(fen),
        crazyData = m.after.position.crazyData,
        eval = none
      )

    val (result, error) = position
      .foldRight(info.variation.take(20), ply)(
        none[Branch],
        (step, acc) =>
          inline def branch = makeBranch(step.move, step.ply)
          acc.fold(branch)(acc => branch.addChild(acc)).some
      )

    error.foreach(e => logChessError(formatError(id, e)))
    result.fold(root)(b => root.addChild(b.setComp))

  private def formatError(id: GameId, err: chess.ErrorStr) =
    s"TreeBuilder https://lichess.org/$id ${err.value.linesIterator.toList.headOption}"
