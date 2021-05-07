package lila.insight

import scala.util.chaining._
import cats.data.NonEmptyList

import chess.format.FEN
import chess.{ Board, Centis, Role, Stats }
import lila.analyse.{ Accuracy, Advice }
import lila.game.{ Game, Pov }

final private class PovToEntry(
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private type Ply = Int

  case class RichPov(
      pov: Pov,
      provisional: Boolean,
      initialFen: Option[FEN],
      analysis: Option[lila.analyse.Analysis],
      division: chess.Division,
      moveAccuracy: Option[List[Int]],
      boards: NonEmptyList[Board],
      movetimes: NonEmptyList[Centis],
      advices: Map[Ply, Advice]
  )

  def apply(game: Game, userId: String, provisional: Boolean): Fu[Either[Game, InsightEntry]] =
    enrich(game, userId, provisional) map
      (_ flatMap convert toRight game)

  private def removeWrongAnalysis(game: Game): Boolean = {
    if (game.metadata.analysed && !game.analysable) {
      gameRepo setUnanalysed game.id
      analysisRepo remove game.id
      true
    } else false
  }

  private def enrich(game: Game, userId: String, provisional: Boolean): Fu[Option[RichPov]] =
    if (removeWrongAnalysis(game)) fuccess(none)
    else
      lila.game.Pov.ofUserId(game, userId) ?? { pov =>
        gameRepo.initialFen(game) zip
          (game.metadata.analysed ?? analysisRepo.byId(game.id)) map { case (fen, an) =>
            for {
              boards <-
                chess.Replay
                  .boards(
                    moveStrs = game.pgnMoves,
                    initialFen = fen,
                    variant = game.variant
                  )
                  .toOption
                  .flatMap(_.toNel)
              movetimes <- game.moveTimes(pov.color).flatMap(_.toNel)
            } yield RichPov(
              pov = pov,
              provisional = provisional,
              initialFen = fen,
              analysis = an,
              division = chess.Divider(boards.toList),
              moveAccuracy = an.map { Accuracy.diffsList(pov, _) },
              boards = boards,
              movetimes = movetimes,
              advices = an.?? {
                _.advices.view.map { a =>
                  a.info.ply -> a
                }.toMap
              }
            )
          }
      }

  private def pgnMoveToRole(pgn: String): Role =
    pgn.head match {
      case 'N'       => chess.Knight
      case 'B'       => chess.Bishop
      case 'R'       => chess.Rook
      case 'Q'       => chess.Queen
      case 'K' | 'O' => chess.King
      case _         => chess.Pawn
    }

  private def makeMoves(from: RichPov): List[InsightMove] = {
    val cpDiffs = ~from.moveAccuracy toVector
    val prevInfos = from.analysis.?? { an =>
      Accuracy.prevColorInfos(from.pov, an) pipe { is =>
        from.pov.color.fold(is, is.map(_.invert))
      }
    }
    val movetimes = from.movetimes.toList
    val roles     = from.pov.game.pgnMoves(from.pov.color) map pgnMoveToRole
    val boards = {
      val pivot = if (from.pov.color == from.pov.game.startColor) 0 else 1
      from.boards.toList.zipWithIndex.collect {
        case (e, i) if (i % 2) == pivot => e
      }
    }
    val blurs = {
      val bools = from.pov.player.blurs.booleans
      bools ++ Array.fill(movetimes.size - bools.length)(false)
    }
    val timeCvs = slidingMoveTimesCvs(movetimes)
    movetimes.zip(roles).zip(boards).zip(blurs).zip(timeCvs).zipWithIndex.map {
      case (((((movetime, role), board), blur), timeCv), i) =>
        val ply      = i * 2 + from.pov.color.fold(1, 2)
        val prevInfo = prevInfos lift i
        val opportunism = from.advices.get(ply - 1) flatMap {
          case o if o.judgment.isBlunder =>
            from.advices get ply match {
              case Some(p) if p.judgment.isBlunder => false.some
              case _                               => true.some
            }
          case _ => none
        }
        val luck = from.advices.get(ply) flatMap {
          case o if o.judgment.isBlunder =>
            from.advices.get(ply + 1) match {
              case Some(p) if p.judgment.isBlunder => true.some
              case _                               => false.some
            }
          case _ => none
        }
        InsightMove(
          phase = Phase.of(from.division, ply),
          tenths = movetime.roundTenths,
          role = role,
          eval = prevInfo.flatMap(_.cp).map(_.ceiled.centipawns),
          mate = prevInfo.flatMap(_.mate).map(_.moves),
          cpl = cpDiffs lift i,
          material = board.materialImbalance * from.pov.color.fold(1, -1),
          opportunism = opportunism,
          luck = luck,
          blur = blur,
          timeCv = timeCv
        )
    }
  }

  private def slidingMoveTimesCvs(movetimes: Seq[Centis]): Seq[Option[Float]] = {
    val sliding = 13 // should be odd
    val nb      = movetimes.size
    if (nb < sliding) Vector.fill(nb)(none[Float])
    else {
      val sides = Vector.fill(sliding / 2)(none[Float])
      val cvs = movetimes
        .sliding(sliding)
        .map { a =>
          // drop outliers
          coefVariation(a.map(_.centis + 10).sorted.drop(1).dropRight(1))
        }
      sides ++ cvs ++ sides
    }
  }

  private def coefVariation(a: Seq[Int]): Option[Float] = {
    val s = Stats(a)
    s.stdDev.map { _ / s.mean }
  }

  private def queenTrade(from: RichPov) =
    QueenTrade {
      from.division.end.fold(from.boards.last.some)(from.boards.toList.lift) match {
        case Some(board) =>
          chess.Color.all.forall { color =>
            !board.hasPiece(chess.Piece(color, chess.Queen))
          }
        case _ =>
          logger.warn(s"https://lichess.org/${from.pov.gameId} missing endgame board")
          false
      }
    }

  private def convert(from: RichPov): Option[InsightEntry] = {
    import from._
    import pov.game
    for {
      myId     <- pov.player.userId
      myRating <- pov.player.rating
      opRating <- pov.opponent.rating
      perfType <- game.perfType
    } yield InsightEntry(
      id = InsightEntry povToId pov,
      number = 0, // temporary :-/ the Indexer will set it
      userId = myId,
      color = pov.color,
      perf = perfType,
      eco =
        if (game.playable || game.turns < 4 || game.fromPosition || game.variant.exotic) none
        else chess.opening.Ecopening fromGame game.pgnMoves.toList,
      myCastling = Castling.fromMoves(game pgnMoves pov.color),
      opponentRating = opRating,
      opponentStrength = RelativeStrength(opRating - myRating),
      opponentCastling = Castling.fromMoves(game pgnMoves !pov.color),
      moves = makeMoves(from),
      queenTrade = queenTrade(from),
      result = game.winnerUserId match {
        case None                 => Result.Draw
        case Some(u) if u == myId => Result.Win
        case _                    => Result.Loss
      },
      termination = Termination fromStatus game.status,
      ratingDiff = ~pov.player.ratingDiff,
      analysed = analysis.isDefined,
      provisional = provisional,
      date = game.createdAt
    )
  }
}
