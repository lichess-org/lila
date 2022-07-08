package lila.insight

import cats.data.NonEmptyList
import chess.format.{ FEN, Forsyth }
import chess.opening.FullOpeningDB
import chess.{ Centis, Clock, Role, Situation, Stats }
import scala.util.chaining._

import lila.analyse.{ AccuracyCP, AccuracyPercent, Advice, WinPercent }
import lila.game.{ Game, Pov }
import lila.user.User
import lila.common.{ LilaOpening, LilaOpeningFamily }

case class RichPov(
    pov: Pov,
    provisional: Boolean,
    analysis: Option[lila.analyse.Analysis],
    situations: NonEmptyList[Situation],
    clock: Clock.Config,
    movetimes: Vector[Centis],
    clockStates: Vector[Centis],
    advices: Map[Ply, Advice]
) {
  lazy val division = chess.Divider(situations.map(_.board).toList)
}

final private class PovToEntry(
    gameRepo: lila.game.GameRepo,
    analysisRepo: lila.analyse.AnalysisRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(game: Game, userId: User.ID, provisional: Boolean): Fu[Either[Game, InsightEntry]] =
    enrich(game, userId, provisional) map
      (_ flatMap convert toRight game)

  private def removeWrongAnalysis(game: Game): Boolean = {
    if (game.metadata.analysed && !game.analysable) {
      gameRepo setUnanalysed game.id
      analysisRepo remove game.id
      true
    } else false
  }

  private def enrich(game: Game, userId: User.ID, provisional: Boolean): Fu[Option[RichPov]] =
    if (removeWrongAnalysis(game)) fuccess(none)
    else
      lila.game.Pov.ofUserId(game, userId) ?? { pov =>
        gameRepo.initialFen(game) zip
          (game.metadata.analysed ?? analysisRepo.byId(game.id)) map { case (fen, an) =>
            for {
              situations <-
                chess.Replay
                  .situations(
                    moveStrs = game.pgnMoves,
                    initialFen = fen orElse {
                      !pov.game.variant.standardInitialPosition option pov.game.variant.initialFen
                    },
                    variant = game.variant
                  )
                  .toOption
                  .flatMap(_.toNel)
              clock       <- game.clock
              movetimes   <- game moveTimes pov.color
              clockStates <- game.clockHistory.map(_(pov.color))
            } yield RichPov(
              pov = pov,
              provisional = provisional,
              analysis = an,
              situations = situations,
              clock = clock.config,
              movetimes = movetimes.toVector,
              clockStates = clockStates,
              advices = an.?? {
                _.advices.view
                  .map { a =>
                    a.info.ply -> a
                  }
                  .toMap
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
    val sideAndStart = from.pov.sideAndStart
    def cpDiffs      = from.analysis ?? { AccuracyCP.diffsList(sideAndStart, _).toVector }
    val accuracyPercents = from.analysis map {
      AccuracyPercent.fromAnalysisAndPov(sideAndStart, _).toVector
    }
    val prevInfos = from.analysis.?? { an =>
      AccuracyCP.prevColorInfos(sideAndStart, an) pipe { is =>
        from.pov.color.fold(is, is.map(_.invert))
      }
    }
    val roles = from.pov.game.pgnMoves(from.pov.color) map pgnMoveToRole
    val situations = {
      val pivot = if (from.pov.color == from.pov.game.startColor) 0 else 1
      from.situations.toList.zipWithIndex.collect {
        case (e, i) if (i % 2) == pivot => e
      }
    }
    val blurs = {
      val bools = from.pov.player.blurs.booleans
      bools ++ Array.fill(from.movetimes.size - bools.length)(false)
    }
    val timeCvs = slidingMoveTimesCvs(from.movetimes)
    from.clockStates.toList
      .zip(from.movetimes)
      .zip(roles)
      .zip(situations)
      .zip(blurs)
      .zip(timeCvs)
      .zipWithIndex
      .map { case ((((((clock, movetime), role), situation), blur), timeCv), i) =>
        val ply      = i * 2 + from.pov.color.fold(1, 2)
        val prevInfo = prevInfos lift i
        val awareness = from.advices.get(ply - 1) flatMap {
          case o if o.judgment.isMistakeOrBlunder =>
            from.advices get ply match {
              case Some(p) if p.judgment.isMistakeOrBlunder => false.some
              case _                                        => true.some
            }
          case _ => none
        }
        val luck = from.advices.get(ply) flatMap {
          case o if o.judgment.isMistakeOrBlunder =>
            from.advices.get(ply + 1) match {
              case Some(p) if p.judgment.isMistakeOrBlunder => true.some
              case _                                        => false.some
            }
          case _ => none
        }
        val accuracyPercent = accuracyPercents flatMap { accs =>
          accs lift i orElse {
            if (i == situations.size - 1) // last eval missing if checkmate
              ~from.pov.win && from.pov.game.status.is(_.Mate) option AccuracyPercent.perfect
            else none // evals can be missing in super long games (300 plies, used to be 200)
          }
        }

        InsightMove(
          phase = Phase.of(from.division, ply),
          tenths = movetime.roundTenths,
          clockPercent = ClockPercent(from.clock, clock),
          role = role,
          eval = prevInfo.flatMap(_.eval.forceAsCp).map(_.ceiled.centipawns),
          cpl = cpDiffs.lift(i).flatten,
          winPercent = prevInfo.map(_.eval) flatMap WinPercent.fromEval,
          accuracyPercent = accuracyPercent,
          material = situation.board.materialImbalance * from.pov.color.fold(1, -1),
          awareness = awareness,
          luck = luck,
          blur = blur,
          timeCv = timeCv
        )
      }
  }

  private def slidingMoveTimesCvs(movetimes: Vector[Centis]): Seq[Option[Float]] = {
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
      from.division.end.fold(from.situations.last.some)(from.situations.toList.lift) match {
        case Some(situation) =>
          chess.Color.all.forall { color =>
            !situation.board.hasPiece(chess.Piece(color, chess.Queen))
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
      perfType <- game.perfType
      myRating = pov.player.stableRating
      opRating = pov.opponent.stableRating
      opening  = findOpening(from)
    } yield InsightEntry(
      id = InsightEntry povToId pov,
      number = 0, // temporary :-/ the Indexer will set it
      userId = myId,
      color = pov.color,
      perf = perfType,
      opening = opening,
      myCastling = Castling.fromMoves(game pgnMoves pov.color),
      rating = myRating,
      opponentRating = opRating,
      opponentStrength = for { m <- myRating; o <- opRating } yield RelativeStrength(o - m),
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

  private def findOpening(from: RichPov): Option[LilaOpening] =
    from.pov.game.variant.standard ??
      from.situations.tail.view
        .takeWhile(_.board.actors.size > 16)
        .foldRight(none[LilaOpening]) {
          case (sit, None) =>
            FullOpeningDB
              .findByFen(FEN(Forsyth exportStandardPositionTurnCastlingEp sit))
              .flatMap(LilaOpening.apply)
          case (_, found) => found
        }
}
