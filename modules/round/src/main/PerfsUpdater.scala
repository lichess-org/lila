package lidraughts.round

import draughts.{ Speed, Color }
import org.goochjs.glicko2._

import lidraughts.game.{ GameRepo, Game, PerfPicker, RatingDiffs }
import lidraughts.history.HistoryApi
import lidraughts.rating.{ Glicko, Perf }
import lidraughts.user.{ UserRepo, User, Perfs, RankingApi }

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
          case draughts.variant.Frisian =>
            updateRatings(ratingsW.frisian, ratingsB.frisian, result, system)
          case draughts.variant.Standard => game.speed match {
            case Speed.Bullet =>
              updateRatings(ratingsW.bullet, ratingsB.bullet, result, system)
            case Speed.Blitz =>
              updateRatings(ratingsW.blitz, ratingsB.blitz, result, system)
            case Speed.Rapid =>
              updateRatings(ratingsW.rapid, ratingsB.rapid, result, system)
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
        val ratingDiffs = Color.Map(
          intRatingLens(perfsW) - intRatingLens(white.perfs),
          intRatingLens(perfsB) - intRatingLens(black.perfs)
        )
        GameRepo.setRatingDiffs(game.id, ratingDiffs) zip
          UserRepo.setPerfs(white, perfsW, white.perfs) zip
          UserRepo.setPerfs(black, perfsB, black.perfs) zip
          historyApi.add(white, game, perfsW) zip
          historyApi.add(black, game, perfsB) zip
          (if (!white.rankban) rankingApi.save(white.id, game.perfType, perfsW) else funit) zip
          (if (!black.rankban) rankingApi.save(black.id, game.perfType, perfsB) else funit) inject ratingDiffs.some
      }
    }

  private final case class Ratings(
      frisian: Rating,
      ultraBullet: Rating,
      bullet: Rating,
      blitz: Rating,
      rapid: Rating,
      classical: Rating,
      correspondence: Rating
  )

  private def mkRatings(perfs: Perfs) = Ratings(
    frisian = perfs.frisian.toRating,
    ultraBullet = perfs.ultraBullet.toRating,
    bullet = perfs.bullet.toRating,
    blitz = perfs.blitz.toRating,
    rapid = perfs.rapid.toRating,
    classical = perfs.classical.toRating,
    correspondence = perfs.correspondence.toRating
  )

  private def resultOf(game: Game): Glicko.Result =
    game.winnerColor match {
      case Some(draughts.White) => Glicko.Result.Win
      case Some(draughts.Black) => Glicko.Result.Loss
      case None => Glicko.Result.Draw
    }

  private def updateRatings(white: Rating, black: Rating, result: Glicko.Result, system: RatingCalculator): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(white, black)
      case Glicko.Result.Win => results.addResult(white, black)
      case Glicko.Result.Loss => results.addResult(black, white)
    }
    try {
      system.updateRatings(results)
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
      frisian = addRatingIf(game.ratingVariant.frisian, perfs.frisian, ratings.frisian),
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
