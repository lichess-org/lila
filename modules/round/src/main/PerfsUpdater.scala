package lila.round

import chess.{ Color, Speed }
import org.goochjs.glicko2._

import lila.game.{ Game, GameRepo, PerfPicker, RatingDiffs }
import lila.history.HistoryApi
import lila.rating.{ Glicko, Perf, RatingFactors, RatingRegulator, PerfType => PT }
import lila.user.{ Perfs, RankingApi, User, UserRepo }

final class PerfsUpdater(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    historyApi: HistoryApi,
    rankingApi: RankingApi,
    botFarming: BotFarming,
    ratingFactors: () => RatingFactors
)(implicit ec: scala.concurrent.ExecutionContext) {

  // returns rating diffs
  def save(game: Game, white: User, black: User): Fu[Option[RatingDiffs]] =
    botFarming(game) flatMap {
      case true => fuccess(none)
      case _ =>
        PerfPicker.main(game) ?? { mainPerf =>
          (game.rated && game.finished && game.accountable && !white.lame && !black.lame) ?? {
            val ratingsW = mkRatings(white.perfs)
            val ratingsB = mkRatings(black.perfs)
            val result   = resultOf(game)
            game.ratingVariant match { // todo variant
              case chess.variant.Standard =>
                game.speed match {
                  case Speed.Bullet =>
                    updateRatings(ratingsW.bullet, ratingsB.bullet, result)
                  case Speed.Blitz =>
                    updateRatings(ratingsW.blitz, ratingsB.blitz, result)
                  case Speed.Rapid =>
                    updateRatings(ratingsW.rapid, ratingsB.rapid, result)
                  case Speed.Classical =>
                    updateRatings(ratingsW.classical, ratingsB.classical, result)
                  case Speed.Correspondence =>
                    updateRatings(ratingsW.correspondence, ratingsB.correspondence, result)
                  case Speed.UltraBullet =>
                    updateRatings(ratingsW.ultraBullet, ratingsB.ultraBullet, result)
                }
              case _ =>
            }
            val perfsW                      = mkPerfs(ratingsW, white -> black, game)
            val perfsB                      = mkPerfs(ratingsB, black -> white, game)
            def intRatingLens(perfs: Perfs) = mainPerf(perfs).glicko.intRating
            val ratingDiffs = Color.Map(
              intRatingLens(perfsW) - intRatingLens(white.perfs),
              intRatingLens(perfsB) - intRatingLens(black.perfs)
            )
            gameRepo.setRatingDiffs(game.id, ratingDiffs) zip
              userRepo.setPerfs(white, perfsW, white.perfs) zip
              userRepo.setPerfs(black, perfsB, black.perfs) zip
              historyApi.add(white, game, perfsW) zip
              historyApi.add(black, game, perfsB) zip
              rankingApi.save(white, game.perfType, perfsW) zip
              rankingApi.save(black, game.perfType, perfsB) inject ratingDiffs.some
          }
        }
    }

  private case class Ratings( // todo variant
      ultraBullet: Rating,
      bullet: Rating,
      blitz: Rating,
      rapid: Rating,
      classical: Rating,
      correspondence: Rating
  )

  private def mkRatings(perfs: Perfs) =
    Ratings(
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
      case None              => Glicko.Result.Draw
    }

  private def updateRatings(white: Rating, black: Rating, result: Glicko.Result): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(white, black)
      case Glicko.Result.Win  => results.addResult(white, black)
      case Glicko.Result.Loss => results.addResult(black, white)
    }
    try {
      Glicko.system.updateRatings(results, true)
    } catch {
      case e: Exception => logger.error("update ratings", e)
    }
  }

  private def mkPerfs(ratings: Ratings, users: (User, User), game: Game): Perfs =
    users match {
      case (player, opponent) =>
        val perfs            = player.perfs
        val speed            = game.speed
        val isStd            = game.ratingVariant.standard
        val isHumanVsMachine = player.noBot && opponent.isBot
        def addRatingIf(cond: Boolean, perf: Perf, rating: Rating) =
          if (cond) {
            val p = perf.addOrReset(_.round.error.glicko, s"game ${game.id}")(rating, game.movedAt)
            if (isHumanVsMachine) p averageGlicko perf // halve rating diffs for human
            else p
          } else perf
        val perfs1 = perfs.copy(
          ultraBullet =
            addRatingIf(isStd && speed == Speed.UltraBullet, perfs.ultraBullet, ratings.ultraBullet),
          bullet = addRatingIf(isStd && speed == Speed.Bullet, perfs.bullet, ratings.bullet),
          blitz = addRatingIf(isStd && speed == Speed.Blitz, perfs.blitz, ratings.blitz),
          rapid = addRatingIf(isStd && speed == Speed.Rapid, perfs.rapid, ratings.rapid),
          classical = addRatingIf(isStd && speed == Speed.Classical, perfs.classical, ratings.classical),
          correspondence =
            addRatingIf(isStd && speed == Speed.Correspondence, perfs.correspondence, ratings.correspondence)
        )
        val r = RatingRegulator(ratingFactors()) _
        val perfs2 = perfs1.copy(
          bullet = r(PT.Bullet, perfs.bullet, perfs1.bullet),
          blitz = r(PT.Blitz, perfs.blitz, perfs1.blitz),
          rapid = r(PT.Rapid, perfs.rapid, perfs1.rapid),
          classical = r(PT.Classical, perfs.classical, perfs1.classical),
          correspondence = r(PT.Correspondence, perfs.correspondence, perfs1.correspondence)
        )
        if (isStd) perfs2.updateStandard else perfs2
    }
}
