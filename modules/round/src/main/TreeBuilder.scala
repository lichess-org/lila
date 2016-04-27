package lila.round

import chess.format.pgn.Pgn
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
    a: Option[(Pgn, Analysis)],
    initialFen: String,
    withOpening: Boolean): Root = apply(
    id = game.id,
    pgnMoves = game.pgnMoves,
    variant = game.variant,
    a = a,
    initialFen = initialFen,
    withOpening = withOpening)

  def apply(
    id: String,
    pgnMoves: List[String],
    variant: Variant,
    a: Option[(Pgn, Analysis)],
    initialFen: String,
    withOpening: Boolean): Root = {
    chess.Replay.gameMoveWhileValid(pgnMoves, initialFen, variant) match {
      case (init, games, error) =>
        error foreach logChessError(id)
        val openingOf: OpeningOf =
          if (withOpening && Variant.openingSensibleVariants(variant)) FullOpeningDB.findByFen
          else _ => None
        val fen = Forsyth >> init
        val infos: Vector[Info] = a.??(_._2.infos.toVector)
        val advices: Map[Ply, Advice] = a.??(_._2.advices.map { a =>
          a.ply -> a
        }.toMap)
        // analysis.advices.foldLeft(steps) {
        //   case (steps, ad) =>
        //     val index = ad.ply - analysis.startPly
        //     (for {
        //       before <- steps lift (index - 1)
        //       after <- steps lift index
        //     } yield steps.updated(index, after.copy(
        //       nag = ad.nag.symbol.some,
        //       comments = ad.makeComment(false, true) :: after.comments,
        //       variations = if (ad.info.variation.isEmpty) after.variations
        //       else makeVariation(gameId, before, ad.info, variant).toList :: after.variations))
        //     ) | steps
        // }
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
            nag = advice.map(_.nag.symbol),
            comments = advice.map(_.makeComment(false, true)).toList)
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
        eval = none,
        nag = none,
        comments = Nil)
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

  //   private def makeVariation(gameId: String, fromStep: Step, info: Info, variant: Variant): List[Step] = {
  //     chess.Replay.gameWhileValid(info.variation take 20, fromStep.fen, variant) match {
  //       case (games, error) =>
  //         error foreach logChessError(gameId)
  //         games.drop(1).map { g =>
  //           Step(
  //             id = g.board.history.lastMove.map(UciCharPair.apply),
  //             ply = g.turns,
  //             move = for {
  //               uci <- g.board.history.lastMove
  //               san <- g.pgnMoves.lastOption
  //             } yield Step.Move(uci, san),
  //             fen = Forsyth >> g,
  //             check = g.situation.check,
  //             dests = None,
  //             drops = None,
  //             crazyData = g.situation.board.crazyData)
  //         }
  //     }
  //   }

  private val logChessError = (id: String) => (err: String) =>
    logger.warn(s"round.TreeBuilder http://lichess.org/$id ${err.lines.toList.headOption}")
}
