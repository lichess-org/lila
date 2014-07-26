package lila.round

import chess.Speed
import org.goochjs.glicko2._
import org.joda.time.DateTime
import play.api.Logger

import lila.game.{ GameRepo, Game, Pov }
import lila.rating.{ Glicko, Perf }
import lila.user.{ UserRepo, User, Perfs }

private final class PerfsUpdater {

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def save(game: Game, white: User, black: User): Funit =
    Perfs.variantLens(game.variant) ?? { perfLens =>
      (game.rated && game.finished && game.accountable && !white.engine && !black.engine) ?? {
        val ratingsW = mkRatings(white.perfs, game.poolId)
        val ratingsB = mkRatings(black.perfs, game.poolId)
        val result = resultOf(game)
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
          game.poolId.fold(Perfs.variantLens(game.variant).fold(perfs.standard)(_(perfs))) { id =>
            perfs.pool(id)
          }.glicko.intRating
        GameRepo.setRatingDiffs(game.id,
          intRatingLens(perfsW) - intRatingLens(white.perfs),
          intRatingLens(perfsB) - intRatingLens(black.perfs)) zip
          UserRepo.setPerfs(white, perfsW) zip
          UserRepo.setPerfs(black, perfsB)
      }.void
    }

  private final case class Ratings(
    standard: Rating,
    chess960: Rating,
    bullet: Rating,
    blitz: Rating,
    slow: Rating,
    white: Rating,
    black: Rating,
    pool: Option[(String, Rating)])

  private def mkRatings(perfs: Perfs, poolId: Option[String]) = new Ratings(
    standard = perfs.standard.toRating,
    chess960 = perfs.chess960.toRating,
    bullet = perfs.bullet.toRating,
    blitz = perfs.blitz.toRating,
    slow = perfs.slow.toRating,
    white = perfs.white.toRating,
    black = perfs.black.toRating,
    pool = poolId map (id => id -> perfs.pool(id).toRating))

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

  private def mkPerfs(ratings: Ratings, perfs: Perfs, pov: Pov): Perfs = {
    import pov._
    val speed = chess.Speed(game.clock)
    val perfs1 = perfs.copy(
      standard = game.variant.standard.fold(perfs.standard add ratings.standard, perfs.standard),
      chess960 = game.variant.chess960.fold(perfs.chess960 add ratings.chess960, perfs.chess960),
      bullet = (speed == Speed.Bullet).fold(perfs.bullet add ratings.bullet, perfs.bullet),
      blitz = (speed == Speed.Blitz).fold(perfs.blitz add ratings.blitz, perfs.blitz),
      slow = (speed == Speed.Slow).fold(perfs.slow add ratings.slow, perfs.slow),
      white = color.white.fold(perfs.white add ratings.white, perfs.white),
      black = color.black.fold(perfs.black add ratings.black, perfs.black))
    ratings.pool.fold(perfs1) {
      case (id, poolRating) => perfs1.copy(
        pools = perfs1.pools + (id -> perfs.pool(id).add(poolRating))
      )
    }
  }

  private def logger = play.api.Logger("PerfsUpdater")
}
