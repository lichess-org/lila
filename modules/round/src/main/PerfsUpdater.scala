package lila.round

import chess.Speed
import org.goochjs.glicko2._
import org.joda.time.DateTime
import play.api.Logger

import lila.game.{ GameRepo, Game, Pov }
import lila.rating.{ Glicko, Perf }
import lila.user.{ UserRepo, HistoryRepo, HistoryEntry, User, Perfs }

private final class PerfsUpdater {

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def save(game: Game, white: User, black: User): Funit =
    (game.rated && game.finished && game.accountable && !white.engine && !black.engine) ?? {
      val ratingsW = mkRatings(white.perfs, game.poolId)
      val ratingsB = mkRatings(black.perfs, game.poolId)
      val result = resultOf(game)
      if (!game.isPool) updateRatings(ratingsW.global, ratingsB.global, result, system)
      updateRatings(ratingsW.white, ratingsB.black, result, system)
      game.variant match {
        case chess.Variant.Standard =>
          updateRatings(ratingsW.standard, ratingsB.standard, result, system)
        case chess.Variant.Chess960 =>
          updateRatings(ratingsW.chess960, ratingsB.chess960, result, system)
        case _ =>
      }
      chess.Speed(game.clock) match {
        case chess.Speed.Bullet =>
          updateRatings(ratingsW.bullet, ratingsB.bullet, result, system)
        case chess.Speed.Blitz =>
          updateRatings(ratingsW.blitz, ratingsB.blitz, result, system)
        case chess.Speed.Slow | chess.Speed.Unlimited =>
          updateRatings(ratingsW.slow, ratingsB.slow, result, system)
      }
      (ratingsW.pool |@| ratingsB.pool) apply {
        case ((_, prW), (_, prB)) => updateRatings(prW, prB, result, system)
      }
      val perfsW = mkPerfs(ratingsW, white.perfs, Pov white game)
      val perfsB = mkPerfs(ratingsB, black.perfs, Pov black game)
      val intRatingLens = (perfs: Perfs) =>
        game.poolId.fold(perfs.global)(id => perfs.pool(id)).glicko.intRating
      ((!game.isPool ?? {
        HistoryRepo.addEntry(white.id, HistoryEntry(
          DateTime.now,
          perfsW.global.glicko.intRating,
          perfsW.global.glicko.intDeviation,
          black.perfs.global.glicko.intRating)) zip
          HistoryRepo.addEntry(black.id, HistoryEntry(
            DateTime.now,
            perfsB.global.glicko.intRating,
            perfsB.global.glicko.intDeviation,
            white.perfs.global.glicko.intRating))
      }) zip
        GameRepo.setRatingDiffs(game.id,
          intRatingLens(perfsW) - intRatingLens(white.perfs),
          intRatingLens(perfsB) - intRatingLens(black.perfs))) >> {
            (makeProgress(white.id) zip makeProgress(black.id)) flatMap {
              case (proW, proB) =>
                (UserRepo.setPerfs(white, perfsW, proW) zip UserRepo.setPerfs(black, perfsB, proB)) void
            }
          }
    }

  private final case class Ratings(
    global: Rating,
    standard: Rating,
    chess960: Rating,
    bullet: Rating,
    blitz: Rating,
    slow: Rating,
    white: Rating,
    black: Rating,
    pool: Option[(String, Rating)])

  private def makeProgress(userId: String): Fu[Int] =
    HistoryRepo.userRatings(userId, -10.some) map { entries =>
      ~((entries.headOption |@| entries.lastOption) {
        case (head, last) => last.rating - head.rating
      })
    }

  private implicit def mkRating(perf: Perf) = new Rating(
    math.max(800, perf.glicko.rating),
    perf.glicko.deviation,
    perf.glicko.volatility,
    perf.nb)

  private def mkRatings(perfs: Perfs, poolId: Option[String]) = new Ratings(
    global = perfs.global,
    standard = perfs.standard,
    chess960 = perfs.chess960,
    bullet = perfs.bullet,
    blitz = perfs.blitz,
    slow = perfs.slow,
    white = perfs.white,
    black = perfs.black,
    pool = poolId map (id => id -> perfs.pool(id)))

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

  private def mkPerf(rating: Rating, latest: Option[DateTime]): Perf = Perf(
    Glicko(rating.getRating, rating.getRatingDeviation, rating.getVolatility),
    nb = rating.getNumberOfResults,
    latest = latest)

  private def mkPerfs(ratings: Ratings, perfs: Perfs, pov: Pov): Perfs = {
    import pov._
    val speed = chess.Speed(game.clock)
    val date = game.createdAt.some
    val perfs1 = perfs.copy(
      global = mkPerf(ratings.global, date),
      standard = mkPerf(ratings.standard, game.variant.standard ? date | perfs.standard.latest),
      chess960 = mkPerf(ratings.chess960, game.variant.chess960 ? date | perfs.standard.latest),
      bullet = mkPerf(ratings.bullet, (speed == Speed.Bullet) ? date | perfs.bullet.latest),
      blitz = mkPerf(ratings.blitz, (speed == Speed.Blitz) ? date | perfs.blitz.latest),
      slow = mkPerf(ratings.slow, (speed == Speed.Slow || speed == Speed.Unlimited) ? date | perfs.slow.latest),
      white = mkPerf(ratings.white, color.white ? date | perfs.white.latest),
      black = mkPerf(ratings.black, color.black ? date | perfs.white.latest))
    ratings.pool match {
      case Some((id, pool)) => perfs1.copy(pools = perfs1.pools + (id -> mkPerf(pool, date)))
      case None             => perfs1
    }
  }

  private def logger = play.api.Logger("PerfsUpdater")
}
