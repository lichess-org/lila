package lila.round

import chess.{ Speed, Color }
import org.goochjs.glicko2._
import org.joda.time.DateTime

import lila.game.{ GameRepo, Game, PerfPicker, RatingDiffs }
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

  // returns rating diffs
  def save(game: Game, white: User, black: User): Fu[Option[RatingDiffs]] =
    PerfPicker.main(game) ?? { mainPerf =>
      (game.rated && game.finished && game.accountable && !white.lame && !black.lame) ?? {
        val ratingsW = mkRatings(white.perfs)
        val ratingsB = mkRatings(black.perfs)
        val result = resultOf(game)
        game.ratingVariant match {
          case chess.variant.Chess960 =>
            updateRatings(ratingsW.chess960, ratingsB.chess960, result, system, game.movedAt)
          case chess.variant.KingOfTheHill =>
            updateRatings(ratingsW.kingOfTheHill, ratingsB.kingOfTheHill, result, system, game.movedAt)
          case chess.variant.ThreeCheck =>
            updateRatings(ratingsW.threeCheck, ratingsB.threeCheck, result, system, game.movedAt)
          case chess.variant.Antichess =>
            updateRatings(ratingsW.antichess, ratingsB.antichess, result, system, game.movedAt)
          case chess.variant.Atomic =>
            updateRatings(ratingsW.atomic, ratingsB.atomic, result, system, game.movedAt)
          case chess.variant.Horde =>
            updateRatings(ratingsW.horde, ratingsB.horde, result, system, game.movedAt)
          case chess.variant.RacingKings =>
            updateRatings(ratingsW.racingKings, ratingsB.racingKings, result, system, game.movedAt)
          case chess.variant.Crazyhouse =>
            updateRatings(ratingsW.crazyhouse, ratingsB.crazyhouse, result, system, game.movedAt)
          case chess.variant.Standard => game.speed match {
            case Speed.Bullet =>
              updateRatings(ratingsW.bullet, ratingsB.bullet, result, system, game.movedAt)
            case Speed.Blitz =>
              updateRatings(ratingsW.blitz, ratingsB.blitz, result, system, game.movedAt)
            case Speed.Rapid =>
              updateRatings(ratingsW.rapid, ratingsB.rapid, result, system, game.movedAt)
            case Speed.Classical =>
              updateRatings(ratingsW.classical, ratingsB.classical, result, system, game.movedAt)
            case Speed.Correspondence =>
              updateRatings(ratingsW.correspondence, ratingsB.correspondence, result, system, game.movedAt)
            case Speed.UltraBullet =>
              updateRatings(ratingsW.ultraBullet, ratingsB.ultraBullet, result, system, game.movedAt)
          }
          case _ =>
        }
        val perfsW = mkPerfs(ratingsW, white.perfs, game)
        val perfsB = mkPerfs(ratingsB, black.perfs, game)
        def intRatingLens(perfs: Perfs) = mainPerf(perfs).glicko.intRating
        val ratingDiffs = Color.Map(
          intRatingLens(perfsW) - intRatingLens(white.perfs),
          intRatingLens(perfsB) - intRatingLens(black.perfs)
        )
        GameRepo.setRatingDiffs(game.id, ratingDiffs) zip
          UserRepo.setPerfs(white, perfsW, white.perfs) zip
          UserRepo.setPerfs(black, perfsB, black.perfs) zip
          historyApi.add(white, game, perfsW) zip
          historyApi.add(black, game, perfsB) zip
          rankingApi.save(white.id, game.perfType, perfsW) zip
          rankingApi.save(black.id, game.perfType, perfsB) inject ratingDiffs.some
      }
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
      rapid: Rating,
      classical: Rating,
      correspondence: Rating
  )

  private def mkRatings(perfs: Perfs) = Ratings(
    chess960 = perfs.chess960.toRating,
    kingOfTheHill = perfs.kingOfTheHill.toRating,
    threeCheck = perfs.threeCheck.toRating,
    antichess = perfs.antichess.toRating,
    atomic = perfs.atomic.toRating,
    horde = perfs.horde.toRating,
    racingKings = perfs.racingKings.toRating,
    crazyhouse = perfs.crazyhouse.toRating,
    ultraBullet = perfs.ultraBullet.toRating,
    bullet = perfs.bullet.toRating,
    blitz = perfs.blitz.toRating,
    rapid = perfs.rapid.toRating,
    classical = perfs.classical.toRating,
    correspondence = perfs.correspondence.toRating
  )

  private def resultOf(game: Game): Glicko.Result =
    game.winnerColor match {
      case Some(chess.White) => Glicko.Result.Win
      case Some(chess.Black) => Glicko.Result.Loss
      case None => Glicko.Result.Draw
    }

  private def updateRatings(white: Rating, black: Rating, result: Glicko.Result, system: RatingCalculator, movedAt: DateTime): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(white, black)
      case Glicko.Result.Win => results.addResult(white, black)
      case Glicko.Result.Loss => results.addResult(black, white)
    }
    try {
      system.updateRatings(results, movedAt, Glicko.ratingPeriodLengthMillis)
    } catch {
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
      rapid = addRatingIf(isStd && speed == Speed.Rapid, perfs.rapid, ratings.rapid),
      classical = addRatingIf(isStd && speed == Speed.Classical, perfs.classical, ratings.classical),
      correspondence = addRatingIf(isStd && speed == Speed.Correspondence, perfs.correspondence, ratings.correspondence)
    )
    if (isStd) perfs1.updateStandard else perfs1
  }
}
