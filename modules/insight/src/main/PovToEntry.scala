package lila.insight

import scala.util.chaining._
import cats.data.NonEmptyList

import shogi.{ Centis, Situation, Stats }
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
      analysis: Option[lila.analyse.Analysis],
      division: shogi.Division,
      moveAccuracy: Option[List[Int]],
      situations: NonEmptyList[Situation],
      movetimes: NonEmptyList[Centis],
      advices: Map[Ply, Advice]
  )

  def apply(game: Game, userId: String, provisional: Boolean): Fu[Either[Game, Entry]] =
    enrich(game, userId, provisional) map
      (_ flatMap convert toRight game)

  private def removeWrongAnalysis(game: Game): Boolean = {
    if (game.metadata.analysed && !game.analysable) {
      gameRepo setUnanalysed game.id
      analysisRepo remove game.id
      true
    }
    false
  }

  private def enrich(game: Game, userId: String, provisional: Boolean): Fu[Option[RichPov]] =
    if (removeWrongAnalysis(game)) fuccess(none)
    else
      lila.game.Pov.ofUserId(game, userId) ?? { pov =>
        (game.metadata.analysed ?? analysisRepo.byId(game.id)) map { an =>
          for {
            situations <-
              shogi.Replay
                .situations(
                  usis = game.usiMoves,
                  initialSfen = game.initialSfen,
                  variant = game.variant
                )
                .toOption
            movetimes <- game.moveTimes(pov.color).flatMap(_.toNel)
          } yield RichPov(
            pov = pov,
            provisional = provisional,
            analysis = an,
            division = shogi.Divider(situations.toList),
            moveAccuracy = an.map { Accuracy.diffsList(pov, _) },
            situations = situations,
            movetimes = movetimes,
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

  private def makeMoves(from: RichPov): List[Move] = {
    val cpDiffs = ~from.moveAccuracy toVector
    val prevInfos = from.analysis.?? { an =>
      Accuracy.prevColorInfos(from.pov, an) pipe { is =>
        from.pov.color.fold(is, is.map(_.invert))
      }
    }
    val movetimes = from.movetimes.toList
    val roles =
      shogi.Replay
        .usiWithRoleWhilePossible(
          from.pov.game.usiMoves,
          from.pov.game.initialSfen,
          from.pov.game.variant
        )
        .map(_.role)
    val situations = {
      val pivot = if (from.pov.color == from.pov.game.startColor) 0 else 1
      from.situations.toList.zipWithIndex.collect {
        case (e, i) if (i % 2) == pivot => e
      }
    }
    val blurs = {
      val bools = from.pov.player.blurs.booleans
      bools ++ Array.fill(movetimes.size - bools.size)(false)
    }
    val timeCvs = slidingMoveTimesCvs(movetimes)
    movetimes.zip(roles).zip(situations).zip(blurs).zip(timeCvs).zipWithIndex.map {
      case (((((movetime, role), sit), blur), timeCv), i) =>
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
        Move(
          phase = Phase.of(from.division, ply),
          tenths = movetime.roundTenths,
          role = role,
          eval = prevInfo.flatMap(_.cp).map(_.ceiled.centipawns),
          mate = prevInfo.flatMap(_.mate).map(_.moves),
          cpl = cpDiffs lift i,
          material = sit.materialImbalance * from.pov.color.fold(1, -1),
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
          coefVariation(a.map(_.centis + 10).toSeq.sorted.drop(1).dropRight(1))
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
        case Some(sit) =>
          shogi.Color.all.forall { color =>
            !sit.board.hasPiece(shogi.Piece(color, shogi.Lance))
          }
        case _ =>
          logger.warn(s"https://lishogi.org/${from.pov.gameId} missing endgame board")
          false
      }
    }

  private def convert(from: RichPov): Option[Entry] = {
    import from._
    import pov.game
    for {
      myId     <- pov.player.userId
      myRating <- pov.player.rating
      opRating <- pov.opponent.rating
      perfType <- game.perfType
    } yield Entry(
      id = Entry povToId pov,
      number = 0, // temporary :/ the Indexer will set it
      userId = myId,
      color = pov.color,
      perf = perfType,
      opponentRating = opRating,
      opponentStrength = RelativeStrength(opRating - myRating),
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
