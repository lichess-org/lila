package lila.round

import chess.Speed
import org.goochjs.glicko2._
import org.joda.time.DateTime
import play.api.Logger

import lila.game.{ GameRepo, Game, Pov }
import lila.user.{ UserRepo, HistoryRepo, HistoryEntry, User, Perf, Perfs, Glicko }

private final class PerfsUpdater {

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d

  val system = new RatingCalculator(VOLATILITY, TAU)

  def save(game: Game, white: User, black: User): Funit =
    (game.rated && game.finished && game.turns >= 2 && !white.engine && !black.engine) ?? {
      val ratingsW = mkRatings(white.perfs)
      val ratingsB = mkRatings(black.perfs)
      val result = resultOf(game)
      updateRatings(ratingsW.global, ratingsB.global, result, system)
      updateRatings(ratingsW.white, ratingsB.black, result, system)
      game.variant match {
        case chess.Variant.Standard ⇒
          updateRatings(ratingsW.standard, ratingsB.standard, result, system)
        case chess.Variant.Chess960 ⇒
          updateRatings(ratingsW.chess960, ratingsB.chess960, result, system)
        case _ ⇒
      }
      chess.Speed(game.clock) match {
        case chess.Speed.Bullet ⇒
          updateRatings(ratingsW.bullet, ratingsB.bullet, result, system)
        case chess.Speed.Blitz ⇒
          updateRatings(ratingsW.blitz, ratingsB.blitz, result, system)
        case chess.Speed.Slow | chess.Speed.Unlimited ⇒
          updateRatings(ratingsW.slow, ratingsB.slow, result, system)
      }
      val perfsW = mkPerfs(ratingsW, white.perfs, Pov white game)
      val perfsB = mkPerfs(ratingsB, black.perfs, Pov black game)
      (HistoryRepo.addEntry(white.id, HistoryEntry(
        DateTime.now,
        perfsW.global.glicko.intRating,
        perfsW.global.glicko.intDeviation,
        black.perfs.global.glicko.intRating)) zip
        HistoryRepo.addEntry(black.id, HistoryEntry(
          DateTime.now,
          perfsB.global.glicko.intRating,
          perfsB.global.glicko.intDeviation,
          white.perfs.global.glicko.intRating)) zip
        GameRepo.setRatingDiffs(game.id,
          perfsW.global.glicko.intRating - white.perfs.global.glicko.intRating,
          perfsB.global.glicko.intRating - black.perfs.global.glicko.intRating)) >> {
            (makeProgress(white.id) zip makeProgress(black.id)) flatMap {
              case (proW, proB) ⇒
                (UserRepo.setPerfs(white, perfsW, proW) zip UserRepo.setPerfs(black, perfsB, proB)) void
            }
          }
    }

  final class Ratings(
    val global: Rating,
    val standard: Rating,
    val chess960: Rating,
    val bullet: Rating,
    val blitz: Rating,
    val slow: Rating,
    val white: Rating,
    val black: Rating)

  def makeProgress(userId: String): Fu[Int] =
    HistoryRepo.userRatings(userId, -10.some) map { entries ⇒
      ~((entries.headOption |@| entries.lastOption) {
        case (head, last) ⇒ last.rating - head.rating
      })
    }

  private implicit def mkRating(perf: Perf) = new Rating(
    perf.glicko.rating, perf.glicko.deviation, perf.glicko.volatility, perf.nb)

  def mkRatings(perfs: Perfs) = new Ratings(
    global = perfs.global,
    standard = perfs.standard,
    chess960 = perfs.chess960,
    bullet = perfs.bullet,
    blitz = perfs.blitz,
    slow = perfs.slow,
    white = perfs.white,
    black = perfs.black)

  def resultOf(game: Game): Glicko.Result =
    game.winnerColor match {
      case Some(chess.White) ⇒ Glicko.Result.Win
      case Some(chess.Black) ⇒ Glicko.Result.Loss
      case None              ⇒ Glicko.Result.Draw
    }

  def updateRatings(white: Rating, black: Rating, result: Glicko.Result, system: RatingCalculator) {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw ⇒ results.addDraw(white, black)
      case Glicko.Result.Win  ⇒ results.addResult(white, black)
      case Glicko.Result.Loss ⇒ results.addResult(black, white)
    }
    try {
      system.updateRatings(results)
    }
    catch {
      case e: Exception ⇒ logger.error(e.getMessage)
    }
  }

  def mkPerf(rating: Rating, latest: Option[DateTime]): Perf = Perf(
    Glicko(rating.getRating, rating.getRatingDeviation, rating.getVolatility),
    nb = rating.getNumberOfResults,
    latest = latest)

  def mkPerfs(ratings: Ratings, perfs: Perfs, pov: Pov): Perfs = {
    import pov._
    val speed = chess.Speed(game.clock)
    perfs.copy(
      global = mkPerf(ratings.global, game.createdAt.some),
      standard = mkPerf(ratings.standard, game.variant.standard ? game.createdAt.some | perfs.standard.latest),
      chess960 = mkPerf(ratings.chess960, game.variant.chess960 ? game.createdAt.some | perfs.standard.latest),
      bullet = mkPerf(ratings.bullet, (speed == Speed.Bullet) ? game.createdAt.some | perfs.bullet.latest),
      blitz = mkPerf(ratings.blitz, (speed == Speed.Blitz) ? game.createdAt.some | perfs.blitz.latest),
      slow = mkPerf(ratings.slow, (speed == Speed.Slow || speed == Speed.Unlimited) ? game.createdAt.some | perfs.slow.latest),
      white = mkPerf(ratings.white, color.white ? game.createdAt.some | perfs.white.latest),
      black = mkPerf(ratings.black, color.black ? game.createdAt.some | perfs.white.latest))
  }

  private def logger = play.api.Logger("PerfsUpdater")
}
