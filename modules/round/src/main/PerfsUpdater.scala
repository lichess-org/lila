package lila.round

import chess.{ Variant, Speed }
import org.goochjs.glicko2._
import org.joda.time.DateTime
import play.api.Logger

import lila.game.{ GameRepo, Game, Pov, PerfPicker }
import lila.history.HistoryApi
import lila.rating.{ Glicko, Perf }
import lila.user.{ UserRepo, User, Perfs }

final class PerfsUpdater(historyApi: HistoryApi) {

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def save(game: Game, white: User, black: User, resetGameRatings: Boolean = false): Funit =
    PerfPicker.main(game) ?? { mainPerf =>
      (game.rated && game.finished && game.accountable && !white.engine && !black.engine) ?? {
        val ratingsW = mkRatings(white.perfs)
        val ratingsB = mkRatings(black.perfs)
        val result = resultOf(game)
        game.variant match {
          case Variant.Chess960 =>
            updateRatings(ratingsW.chess960, ratingsB.chess960, result, system)
          case Variant.KingOfTheHill =>
            updateRatings(ratingsW.kingOfTheHill, ratingsB.kingOfTheHill, result, system)
          case Variant.ThreeCheck =>
            updateRatings(ratingsW.threeCheck, ratingsB.threeCheck, result, system)
          case Variant.Antichess =>
            updateRatings(ratingsW.antichess, ratingsB.antichess, result, system)
          case Variant.AtomicChess =>
            updateRatings(ratingsW.atomicChess, ratingsB.atomicChess, result, system)
          case Variant.Standard => game.speed match {
            case Speed.Bullet =>
              updateRatings(ratingsW.bullet, ratingsB.bullet, result, system)
            case Speed.Blitz =>
              updateRatings(ratingsW.blitz, ratingsB.blitz, result, system)
            case Speed.Classical =>
              updateRatings(ratingsW.classical, ratingsB.classical, result, system)
            case Speed.Correspondence =>
              updateRatings(ratingsW.correspondence, ratingsB.correspondence, result, system)
          }
          case _ =>
        }
        val perfsW = mkPerfs(ratingsW, white.perfs, game)
        val perfsB = mkPerfs(ratingsB, black.perfs, game)
        def intRatingLens(perfs: Perfs) = mainPerf(perfs).glicko.intRating
        resetGameRatings.fold(
          GameRepo.setRatingAndDiffs(game.id,
            intRatingLens(white.perfs) -> (intRatingLens(perfsW) - intRatingLens(white.perfs)),
            intRatingLens(black.perfs) -> (intRatingLens(perfsB) - intRatingLens(black.perfs))),
          GameRepo.setRatingDiffs(game.id,
            intRatingLens(perfsW) - intRatingLens(white.perfs),
            intRatingLens(perfsB) - intRatingLens(black.perfs))
        ) zip
          UserRepo.setPerfs(white, perfsW, white.perfs) zip
          UserRepo.setPerfs(black, perfsB, black.perfs) zip
          historyApi.add(white, game, perfsW) zip
          historyApi.add(black, game, perfsB)
      }.void
    }

  private final case class Ratings(
    chess960: Rating,
    kingOfTheHill: Rating,
    threeCheck: Rating,
    antichess: Rating,
    atomicChess: Rating,
    bullet: Rating,
    blitz: Rating,
    classical: Rating,
    correspondence: Rating)

  private def mkRatings(perfs: Perfs) = new Ratings(
    chess960 = perfs.chess960.toRating,
    kingOfTheHill = perfs.kingOfTheHill.toRating,
    threeCheck = perfs.threeCheck.toRating,
    antichess = perfs.antichess.toRating,
    atomicChess = perfs.atomicChess.toRating,
    bullet = perfs.bullet.toRating,
    blitz = perfs.blitz.toRating,
    classical = perfs.classical.toRating,
    correspondence = perfs.correspondence.toRating)

  private def resultOf(game: Game): Glicko.Result =
    game.winnerColor match {
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None              => Glicko.Result.Draw
    }

  private def updateRatings(white: Rating, black: Rating, result: Glicko.Result, system: RatingCalculator) {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(white, black)
      case Glicko.Result.Win  => results.addResult(white, black)
      case Glicko.Result.Loss => results.addResult(black, white)
    }
    try {
      system.updateRatings(results)
    }
    catch {
      case e: Exception => logger.error(e.getMessage)
    }
  }

  private def mkPerfs(ratings: Ratings, perfs: Perfs, game: Game): Perfs = {
    val speed = game.speed
    val isStd = game.variant.standard
    val date = game.updatedAt | game.createdAt
    val perfs1 = perfs.copy(
      chess960 = game.variant.chess960.fold(perfs.chess960.add(ratings.chess960, date), perfs.chess960),
      kingOfTheHill = game.variant.kingOfTheHill.fold(perfs.kingOfTheHill.add(ratings.kingOfTheHill, date), perfs.kingOfTheHill),
      threeCheck = game.variant.threeCheck.fold(perfs.threeCheck.add(ratings.threeCheck, date), perfs.threeCheck),
      antichess = game.variant.antichess.fold(perfs.antichess.add(ratings.antichess, date), perfs.antichess),
      atomicChess = game.variant.atomicChess.fold(perfs.atomicChess.add(ratings.atomicChess, date), perfs.atomicChess),
      bullet = (isStd && speed == Speed.Bullet).fold(perfs.bullet.add(ratings.bullet, date), perfs.bullet),
      blitz = (isStd && speed == Speed.Blitz).fold(perfs.blitz.add(ratings.blitz, date), perfs.blitz),
      classical = (isStd && speed == Speed.Classical).fold(perfs.classical.add(ratings.classical, date), perfs.classical),
      correspondence = (isStd && speed == Speed.Correspondence).fold(perfs.correspondence.add(ratings.correspondence, date), perfs.correspondence))
    if (isStd) perfs1.updateStandard else perfs1
  }

  private def logger = play.api.Logger("PerfsUpdater")
}
