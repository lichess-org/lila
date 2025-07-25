package lila.insight

import chess.format.pgn.SanStr
import chess.opening.OpeningDb
import chess.{ Position, Centis, Clock, Ply, Role, Stats }
import chess.eval.WinPercent

import lila.analyse.{ AccuracyCP, AccuracyPercent, Advice, Analysis }
import lila.common.SimpleOpening
import lila.game.Blurs.booleans

case class RichPov(
    pov: Pov,
    provisional: Boolean,
    analysis: Option[lila.analyse.Analysis],
    boards: NonEmptyList[Position],
    clock: Option[Clock.Config],
    movetimes: Option[Vector[Centis]],
    clockStates: Option[Vector[Centis]],
    advices: Map[Ply, Advice]
):
  lazy val division = chess.Divider(boards.toList.map(_.board))

final private class PovToEntry(
    gameRepo: lila.game.GameRepo,
    gameApi: lila.core.game.GameApi,
    analysisRepo: lila.analyse.AnalysisRepo
)(using Executor):

  def apply(game: Game, userId: UserId, provisional: Boolean): Fu[Either[Game, InsightEntry]] =
    enrich(game, userId, provisional).map(_.flatMap(convert).toRight(game))

  private def removeWrongAnalysis(game: Game): Boolean =
    if game.metadata.analysed && !gameApi.analysable(game) then
      gameRepo.setAnalysed(game.id, false)
      analysisRepo.remove(game.id)
      true
    else false

  private def enrich(game: Game, userId: UserId, provisional: Boolean): Fu[Option[RichPov]] =
    if removeWrongAnalysis(game) then fuccess(none)
    else
      Pov(game, userId).so: pov =>
        gameRepo
          .initialFen(game)
          .zip(game.metadata.analysed.so(analysisRepo.byId(Analysis.Id(game.id))))
          .map { (fen, an) =>
            chess
              .Position(
                game.variant,
                fen.orElse((!pov.game.variant.standardInitialPosition).option(pov.game.variant.initialFen))
              )
              .playPositions(game.sans)
              .toOption
              .flatMap(_.toNel)
              .map: boards =>
                RichPov(
                  pov = pov,
                  provisional = provisional,
                  analysis = an,
                  boards = boards,
                  clock = game.clock.map(_.config),
                  movetimes = game.clock
                    .flatMap(_ => lila.game.GameExt.computeMoveTimes(game, pov.color))
                    .map(_.toVector),
                  clockStates = game.clockHistory.map(_(pov.color)),
                  advices = an.so(_.advices.mapBy(_.info.ply))
                )
          }

  private def sanToRole(san: SanStr): Role =
    san.value.head match
      case 'N' => chess.Knight
      case 'B' => chess.Bishop
      case 'R' => chess.Rook
      case 'Q' => chess.Queen
      case 'K' | 'O' => chess.King
      case _ => chess.Pawn

  private def makeMoves(from: RichPov): List[InsightMove] =
    val sideAndStart = from.pov.sideAndStart
    def cpDiffs = from.analysis.so { AccuracyCP.diffsList(sideAndStart, _).toVector }
    val accuracyPercents = from.analysis.map:
      AccuracyPercent.fromAnalysisAndPov(sideAndStart, _).toVector
    val prevInfos = from.analysis.so { an =>
      AccuracyCP.prevColorInfos(sideAndStart, an).pipe { is =>
        from.pov.color.fold(is, is.map(_.invert))
      }
    }
    val roles = from.pov.game.sansOf(from.pov.color).map(sanToRole)
    val boards =
      val pivot = if from.pov.color == from.pov.game.startColor then 0 else 1
      from.boards.toList.zipWithIndex.collect:
        case (e, i) if (i % 2) == pivot => e
    val blurs =
      val bools = from.pov.player.blurs.booleans
      bools ++ Array.fill(roles.size - bools.length)(false)
    val timeCvs = from.movetimes.map(slidingMoveTimesCvs)
    roles.toList
      .zip(boards)
      .zip(blurs)
      .zip(timeCvs | Vector.fill(roles.size)(none))
      .zip(from.clockStates.map(_.map(some)) | Vector.fill(roles.size)(none))
      .zip(from.movetimes.map(_.map(some)) | Vector.fill(roles.size)(none))
      .mapWithIndex { case ((((((role, board), blur), timeCv), clock), movetime), i) =>
        val ply = Ply(i * 2 + from.pov.color.fold(1, 2))
        val prevInfo = prevInfos.lift(i)
        val awareness = from.advices
          .get(ply - 1)
          .flatMap:
            case o if o.judgment.isMistakeOrBlunder =>
              from.advices.get(ply) match
                case Some(p) if p.judgment.isMistakeOrBlunder => false.some
                case _ => true.some
            case _ => none
        val luck = from.advices
          .get(ply)
          .flatMap:
            case o if o.judgment.isMistakeOrBlunder =>
              from.advices.get(ply + 1) match
                case Some(p) if p.judgment.isMistakeOrBlunder => true.some
                case _ => false.some
            case _ => none
        val accuracyPercent = accuracyPercents.flatMap { accs =>
          accs
            .lift(i)
            .orElse:
              if i == boards.size - 1 then // last eval missing if checkmate
                (~from.pov.win && from.pov.game.status.is(_.Mate)).option(AccuracyPercent.perfect)
              else none // evals can be missing in super long games (300 plies, used to be 200)
        }

        InsightMove(
          phase = Phase.of(from.division, ply),
          tenths = movetime.map(_.roundTenths),
          clockPercent = from.clock.flatMap(clk => clock.map(ClockPercent(clk, _))),
          role = role,
          eval = prevInfo.flatMap(_.eval.forceAsCp).map(_.ceiled.centipawns),
          cpl = cpDiffs.lift(i).flatten,
          winPercent = prevInfo.map(_.eval).flatMap(_.score).map(WinPercent.fromScore),
          accuracyPercent = accuracyPercent,
          material = board.materialImbalance * from.pov.color.fold(1, -1),
          awareness = awareness,
          luck = luck,
          blur = blur,
          timeCv = timeCv
        )
      }

  private def slidingMoveTimesCvs(movetimes: Vector[Centis]): Seq[Option[Float]] =
    val sliding = 13 // should be odd
    val nb = movetimes.size
    if nb < sliding then Vector.fill(nb)(none[Float])
    else
      val sides = Vector.fill(sliding / 2)(none[Float])
      val cvs = movetimes
        .sliding(sliding)
        .map { a =>
          // drop outliers
          coefVariation(a.map(_.centis + 10).sorted.drop(1).dropRight(1))
        }
      sides ++ cvs ++ sides

  private def coefVariation(a: Seq[Int]): Option[Float] =
    val s = Stats(a)
    s.stdDev.map { _ / s.mean }

  private def queenTrade(from: RichPov) = QueenTrade:
    from.division.end.map(_.value).fold(from.boards.last.some)(from.boards.toList.lift) match
      case Some(board) =>
        Color.all.forall(color => !board.contains(color, chess.Queen))
      case _ =>
        logger.warn(s"https://lichess.org/${from.pov.gameId} missing endgame board")
        false

  private def convert(from: RichPov): Option[InsightEntry] =
    import from.*
    import pov.game
    for
      myId <- pov.player.userId
      myRating = pov.player.stableRating
      opRating = pov.opponent.stableRating
      opening = findOpening(from)
    yield InsightEntry(
      id = InsightEntry.povToId(pov),
      userId = myId,
      color = pov.color,
      perf = game.perfKey,
      opening = opening,
      myCastling = Castling.fromMoves(game.sansOf(pov.color)),
      rating = myRating,
      opponentRating = opRating,
      opponentStrength = for m <- myRating; o <- opRating yield RelativeStrength(m, o),
      opponentCastling = Castling.fromMoves(game.sansOf(!pov.color)),
      moves = makeMoves(from),
      queenTrade = queenTrade(from),
      result = game.winnerUserId match
        case None => Result.Draw
        case Some(u) if u == myId => Result.Win
        case _ => Result.Loss
      ,
      termination = Termination.fromStatus(game.status),
      ratingDiff = ~pov.player.ratingDiff,
      analysed = analysis.isDefined,
      provisional = provisional,
      source = game.source,
      date = game.createdAt
    )

  private def findOpening(from: RichPov): Option[SimpleOpening] =
    from.pov.game.variant.standard.so(
      OpeningDb
        .searchInPositions(from.boards)
        .map(_.opening)
        .flatMap(SimpleOpening.apply)
    )
