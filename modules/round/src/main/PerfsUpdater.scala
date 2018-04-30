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

  // returns rating diffs
  def save(game: Game, white: User, black: User): Fu[Option[RatingDiffs]] =
    PerfPicker.main(game) ?? { mainPerf =>
      (game.rated && game.finished && game.accountable && !white.lame && !black.lame) ?? {
        val ratingsW = mkRatings(white.perfs)
        val ratingsB = mkRatings(black.perfs)
        val result = resultOf(game)
        def ur(white: Rating, black: Rating): Unit = {
          updateRatings(white, black, result, game.movedAt)
        }
        game.ratingVariant match {
          case chess.variant.Chess960 => ur(ratingsW.chess960, ratingsB.chess960)
          case chess.variant.KingOfTheHill => ur(ratingsW.kingOfTheHill, ratingsB.kingOfTheHill)
          case chess.variant.ThreeCheck => ur(ratingsW.threeCheck, ratingsB.threeCheck)
          case chess.variant.Antichess => ur(ratingsW.antichess, ratingsB.antichess)
          case chess.variant.Atomic => ur(ratingsW.atomic, ratingsB.atomic)
          case chess.variant.Horde => ur(ratingsW.horde, ratingsB.horde)
          case chess.variant.RacingKings => ur(ratingsW.racingKings, ratingsB.racingKings)
          case chess.variant.Crazyhouse => ur(ratingsW.crazyhouse, ratingsB.crazyhouse)
          case chess.variant.Standard => game.speed match {
            case Speed.Bullet => ur(ratingsW.bullet, ratingsB.bullet)
            case Speed.Blitz => ur(ratingsW.blitz, ratingsB.blitz)
            case Speed.Rapid => ur(ratingsW.rapid, ratingsB.rapid)
            case Speed.Classical => ur(ratingsW.classical, ratingsB.classical)
            case Speed.Correspondence => ur(ratingsW.correspondence, ratingsB.correspondence)
            case Speed.UltraBullet => ur(ratingsW.ultraBullet, ratingsB.ultraBullet)
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

  private def updateRatings(white: Rating, black: Rating, result: Glicko.Result, movedAt: DateTime): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(white, black)
      case Glicko.Result.Win => results.addResult(white, black)
      case Glicko.Result.Loss => results.addResult(black, white)
    }
    try {
      Glicko.system.updateRatings(results, movedAt)
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
