package lidraughts.round

import draughts.Centis
import draughts.format.pdn.Glyphs
import draughts.format.{ Forsyth, FEN, Uci, UciCharPair }
import draughts.opening._
import draughts.variant.Variant
import JsonView.WithFlags
import lidraughts.analyse.{ Analysis, Info, Advice }
import lidraughts.tree._

object TreeBuilder {

  private type Ply = Int
  private type OpeningOf = String => Option[FullOpening]

  private def makeEval(info: Info) = Eval(
    cp = info.cp,
    win = info.win,
    best = info.best
  )

  def apply(
    game: lidraughts.game.Game,
    analysis: Option[Analysis],
    initialFen: FEN,
    withFlags: WithFlags,
    mergeCapts: Boolean
  ): Root = apply(
    id = game.id,
    pdnmoves = game.pdnMoves,
    variant = game.variant,
    analysis = analysis,
    initialFen = initialFen,
    withFlags = withFlags,
    clocks = withFlags.clocks ?? game.bothClockStates(mergeCapts),
    iteratedCapts = game.imported,
    mergeCapts = mergeCapts
  )

  def apply(
    id: String,
    pdnmoves: Vector[String],
    variant: Variant,
    analysis: Option[Analysis],
    initialFen: FEN,
    withFlags: WithFlags,
    clocks: Option[Vector[Centis]],
    iteratedCapts: Boolean = false,
    mergeCapts: Boolean = false
  ): Root = {
    val withClocks: Option[Vector[Centis]] = withFlags.clocks ?? clocks
    draughts.Replay.gameMoveWhileValid(pdnmoves, initialFen.value, variant, iteratedCapts) match {
      case (init, games, error) =>
        error foreach logDraughtsError(id)
        val openingOf: OpeningOf =
          if (withFlags.opening && Variant.openingSensibleVariants(variant)) FullOpeningDB.findByFen
          else _ => None
        val fen = Forsyth >> init
        val infos: Vector[Info] = analysis.??(_.infos.toVector)
        val advices: Map[Ply, Advice] = analysis.??(_.advices.map { a =>
          a.ply -> a
        }(scala.collection.breakOut))
        val root = Root(
          ply = init.turns,
          fen = fen,
          captureLength = init.situation.allMovesCaptureLength.some,
          opening = openingOf(fen),
          clock = withClocks.flatMap(_.headOption),
          eval = infos lift 0 map makeEval
        )

        def makeBranch(index: Int, g: draughts.DraughtsGame, m: Uci.WithSan) = {
          val fen = Forsyth >> g
          val info = infos lift (g.displayTurns - g.startedAtTurn - 1)
          val advice = advices get g.displayTurns
          val branch = Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            captureLength = if (g.situation.ghosts > 0) g.situation.captureLengthFrom(m.uci.origDest._2) else g.situation.allMovesCaptureLength.some,
            opening = openingOf(fen),
            clock = withClocks flatMap (_ lift (index - 1)),
            eval = info map makeEval,
            glyphs = Glyphs.fromList(advice.map(_.judgment.glyph).toList),
            comments = Node.Comments {
              advice.map(_.makeComment(false, true)).toList.map { text =>
                Node.Comment(
                  Node.Comment.Id.make,
                  Node.Comment.Text(text),
                  Node.Comment.Author.Lidraughts
                )
              }
            }
          )
          if (g.situation.ghosts != 0) branch
          else advices.get(g.displayTurns + 1).flatMap { adv =>
            games.lift(index - 1).map {
              case (fromGame, _) =>
                val fromFen = FEN(Forsyth >> fromGame)
                withAnalysisChild(id, branch, variant, fromFen, openingOf)(adv.info)
            }
          } getOrElse branch
        }
        def truncateGhosts(moveList: List[((draughts.DraughtsGame, Uci.WithSan), Int)], mergeCapts: Boolean): Option[Branch] = moveList match {
          case ((g, m), i) :: rest =>
            if (mergeCapts && g.situation.ghosts > 0) truncateGhosts(rest, mergeCapts)
            else rest.foldLeft(makeBranch(i + 1, g, m)) {
              case (node, ((g, m), i)) =>
                val shortUci = Uci(m.uci.shortUci).get
                if (mergeCapts && node.move.uci.origDest._1 == shortUci.origDest._2)
                  node.copy(
                    id = UciCharPair.combine(m.uci, node.move.uci),
                    move = Uci.WithSan(Uci(m.uci.uci.take(2) + node.move.uci.uci).get, Uci.combineSan(m.san, node.move.san))
                  )
                else makeBranch(i + 1, g, m) prependChild node
            } some
          case Nil => None
        }
        games.zipWithIndex.reverse match {
          case revMoves if revMoves.nonEmpty => root prependChild truncateGhosts(revMoves, mergeCapts)
          case _ => root
        }
    }
  }

  private def withAnalysisChild(id: String, root: Branch, variant: Variant, fromFen: FEN, openingOf: OpeningOf)(info: Info): Branch = {
    def makeBranch(index: Int, g: draughts.DraughtsGame, m: Uci.WithSan) = {
      val fen = Forsyth >> g
      Branch(
        id = UciCharPair(m.uci),
        ply = g.turns,
        move = m,
        fen = fen,
        opening = openingOf(fen),
        eval = none
      )
    }
    draughts.Replay.gameMoveWhileValid(info.variation take 20, fromFen.value, variant, true) match {
      case (init, games, error) =>
        error foreach logDraughtsError(id)
        games.zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest => root addChild rest.foldLeft(makeBranch(i + 1, g, m)) {
            case (node, ((g, m), i)) => makeBranch(i + 1, g, m) addChild node
          }.setComp
        }
    }
  }

  private val logDraughtsError = (id: String) => (err: String) =>
    logger.warn(s"round.TreeBuilder https://lidraughts.org/$id ${err.lines.toList.headOption}")
}
