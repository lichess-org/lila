package lila.round

import chess.{ Speed }
import org.goochjs.glicko2._
import org.joda.time.DateTime

import lila.game.{ GameRepo, Game, PerfPicker }
import lila.history.HistoryApi
import lila.rating.{ Glicko, Perf }
import lila.user.{ UserRepo, User, Perfs, RankingApi }

final class PerfsUpdater(
    historyApi: HistoryApi,
    rankingApi: RankingApi
) {

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def save(game: Game, white: User, black: User, resetGameRatings: Boolean = false): Funit =
    PerfPicker.main(game) ?? { mainPerf =>
      (game.rated && game.finished && game.accountable && !white.lame && !black.lame) ?? {
        val ratingsW = mkRatings(white.perfs, game.createdAt)
        val ratingsB = mkRatings(black.perfs, game.createdAt)
        val result = resultOf(game)
        game.ratingVariant match {
          case chess.variant.Chess960 =>
            updateRatings(ratingsW.chess960, ratingsB.chess960, result, system)
          case chess.variant.KingOfTheHill =>
            updateRatings(ratingsW.kingOfTheHill, ratingsB.kingOfTheHill, result, system)
          case chess.variant.ThreeCheck =>
            updateRatings(ratingsW.threeCheck, ratingsB.threeCheck, result, system)
          case chess.variant.Antichess =>
            updateRatings(ratingsW.antichess, ratingsB.antichess, result, system)
          case chess.variant.Atomic =>
            updateRatings(ratingsW.atomic, ratingsB.atomic, result, system)
          case chess.variant.Horde =>
            updateRatings(ratingsW.horde, ratingsB.horde, result, system)
          case chess.variant.RacingKings =>
            updateRatings(ratingsW.racingKings, ratingsB.racingKings, result, system)
          case chess.variant.Crazyhouse =>
            updateRatings(ratingsW.crazyhouse, ratingsB.crazyhouse, result, system)
          case chess.variant.Standard => game.speed match {
            case Speed.Bullet =>
              updateRatings(ratingsW.bullet, ratingsB.bullet, result, system)
            case Speed.Blitz =>
              updateRatings(ratingsW.blitz, ratingsB.blitz, result, system)
            case Speed.Classical =>
              updateRatings(ratingsW.classical, ratingsB.classical, result, system)
            case Speed.Correspondence =>
              updateRatings(ratingsW.correspondence, ratingsB.correspondence, result, system)
            case Speed.UltraBullet =>
              updateRatings(ratingsW.ultraBullet, ratingsB.ultraBullet, result, system)
          }
          case _ =>
        }
        val perfsW = mkPerfs(ratingsW, white.perfs, game)
        val perfsB = mkPerfs(ratingsB, black.perfs, game)
        def intRatingLens(perfs: Perfs) = mainPerf(perfs).glicko.intRating
        resetGameRatings.fold(
          GameRepo.setRatingAndDiffs(
            game.id,
            intRatingLens(white.perfs) -> (intRatingLens(perfsW) - intRatingLens(white.perfs)),
            intRatingLens(black.perfs) -> (intRatingLens(perfsB) - intRatingLens(black.perfs))
          ),
          GameRepo.setRatingDiffs(
            game.id,
            intRatingLens(perfsW) - intRatingLens(white.perfs),
            intRatingLens(perfsB) - intRatingLens(black.perfs)
          )
        ) zip
          UserRepo.setPerfs(white, perfsW, white.perfs) zip
          UserRepo.setPerfs(black, perfsB, black.perfs) zip
          historyApi.add(white, game, perfsW) zip
          historyApi.add(black, game, perfsB) zip
          rankingApi.save(white.id, game.perfType, perfsW) zip
          rankingApi.save(black.id, game.perfType, perfsB)
      }.void
    }

  private final case class Ratings(
    chess960: Rating,
    kingOfTheHill: Rating,
    threeCheck: Rating,
    antichess: Rating,
    atomic: Rating,
    horde: Rating,
    racingKings: Rating,
    crazyhouse: Rating,
    ultraBullet: Rating,
    bullet: Rating,
    blitz: Rating,
    classical: Rating,
    correspondence: Rating
  )

  private def mkRatings(perfs: Perfs, date: DateTime) = Ratings(
    chess960 = perfs.chess960.toRating(date),
    kingOfTheHill = perfs.kingOfTheHill.toRating(date),
    threeCheck = perfs.threeCheck.toRating(date),
    antichess = perfs.antichess.toRating(date),
    atomic = perfs.atomic.toRating(date),
    horde = perfs.horde.toRating(date),
    racingKings = perfs.racingKings.toRating(date),
    crazyhouse = perfs.crazyhouse.toRating(date),
    ultraBullet = perfs.ultraBullet.toRating(date),
    bullet = perfs.bullet.toRating(date),
    blitz = perfs.blitz.toRating(date),
    classical = perfs.classical.toRating(date),
    correspondence = perfs.correspondence.toRating(date)
  )

  private def resultOf(game: Game): Glicko.Result =
    game.winnerColor match {
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None => Glicko.Result.Draw
    }

  private def updateRatings(white: Rating, black: Rating, result: Glicko.Result, system: RatingCalculator) {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(white, black)
      case Glicko.Result.Win => results.addResult(white, black)
      case Glicko.Result.Loss => results.addResult(black, white)
    }
    try {
      system.updateRatings(results)
    }
    catch {
      case e: Exception => logger.error("update ratings", e)
    }
  }

  private def mkPerfs(ratings: Ratings, perfs: Perfs, game: Game): Perfs = {
    val speed = game.speed
    val isStd = game.ratingVariant.standard
    def addRatingIf(cond: Boolean, perf: Perf, rating: Rating) =
      if (cond) perf.addOrReset(_.round.error.glicko, s"game ${game.id}")(rating, game.movedAt)
      else perf
    val perfs1 = perfs.copy(
      chess960 = addRatingIf(game.ratingVariant.chess960, perfs.chess960, ratings.chess960),
      kingOfTheHill = addRatingIf(game.ratingVariant.kingOfTheHill, perfs.kingOfTheHill, ratings.kingOfTheHill),
      threeCheck = addRatingIf(game.ratingVariant.threeCheck, perfs.threeCheck, ratings.threeCheck),
      antichess = addRatingIf(game.ratingVariant.antichess, perfs.antichess, ratings.antichess),
      atomic = addRatingIf(game.ratingVariant.atomic, perfs.atomic, ratings.atomic),
      horde = addRatingIf(game.ratingVariant.horde, perfs.horde, ratings.horde),
      racingKings = addRatingIf(game.ratingVariant.racingKings, perfs.racingKings, ratings.racingKings),
      crazyhouse = addRatingIf(game.ratingVariant.crazyhouse, perfs.crazyhouse, ratings.crazyhouse),
      ultraBullet = addRatingIf(isStd && speed == Speed.UltraBullet, perfs.ultraBullet, ratings.ultraBullet),
      bullet = addRatingIf(isStd && speed == Speed.Bullet, perfs.bullet, ratings.bullet),
      blitz = addRatingIf(isStd && speed == Speed.Blitz, perfs.blitz, ratings.blitz),
      classical = addRatingIf(isStd && speed == Speed.Classical, perfs.classical, ratings.classical),
      correspondence = addRatingIf(isStd && speed == Speed.Correspondence, perfs.correspondence, ratings.correspondence)
    )
    if (isStd) perfs1.updateStandard else perfs1
  }
}
