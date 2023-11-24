package lila.round

import shogi.{ Color, Speed }
import org.goochjs.glicko2._

import lila.game.{ Game, GameRepo, PerfPicker, RatingDiffs }
import lila.history.HistoryApi
import lila.rating.{ Glicko, Perf, PerfType => PT, RatingFactors, RatingRegulator }
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
  def save(game: Game, sente: User, gote: User): Fu[Option[RatingDiffs]] =
    botFarming(game) flatMap {
      case true => fuccess(none)
      case _ =>
        PerfPicker.main(game) ?? { mainPerf =>
          (game.rated && game.finished && game.accountable && !sente.lame && !gote.lame) ?? {
            val ratingsW = mkRatings(sente.perfs)
            val ratingsB = mkRatings(gote.perfs)
            val result   = resultOf(game)
            game.variant match {
              case shogi.variant.Checkshogi =>
                updateRatings(ratingsW.checkshogi, ratingsB.checkshogi, result)
              case shogi.variant.Kyotoshogi =>
                updateRatings(ratingsW.kyotoshogi, ratingsB.kyotoshogi, result)
              case shogi.variant.Annanshogi =>
                updateRatings(ratingsW.annanshogi, ratingsB.annanshogi, result)
              case shogi.variant.Chushogi =>
                updateRatings(ratingsW.chushogi, ratingsB.chushogi, result)
              case shogi.variant.Minishogi =>
                updateRatings(ratingsW.minishogi, ratingsB.minishogi, result)
              case shogi.variant.Standard =>
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
            val perfsW                      = mkPerfs(ratingsW, sente -> gote, game)
            val perfsB                      = mkPerfs(ratingsB, gote -> sente, game)
            def intRatingLens(perfs: Perfs) = mainPerf(perfs).glicko.intRating
            val ratingDiffs = Color.Map(
              intRatingLens(perfsW) - intRatingLens(sente.perfs),
              intRatingLens(perfsB) - intRatingLens(gote.perfs)
            )
            gameRepo.setRatingDiffs(game.id, ratingDiffs) zip
              userRepo.setPerfs(sente, perfsW, sente.perfs) zip
              userRepo.setPerfs(gote, perfsB, gote.perfs) zip
              historyApi.add(sente, game, perfsW) zip
              historyApi.add(gote, game, perfsB) zip
              rankingApi.save(sente, game.perfType, perfsW) zip
              rankingApi.save(gote, game.perfType, perfsB) inject ratingDiffs.some
          }
        }
    }

  private case class Ratings(
      minishogi: Rating,
      chushogi: Rating,
      annanshogi: Rating,
      kyotoshogi: Rating,
      checkshogi: Rating,
      ultraBullet: Rating,
      bullet: Rating,
      blitz: Rating,
      rapid: Rating,
      classical: Rating,
      correspondence: Rating
  )

  private def mkRatings(perfs: Perfs) =
    Ratings(
      minishogi = perfs.minishogi.toRating,
      chushogi = perfs.chushogi.toRating,
      annanshogi = perfs.annanshogi.toRating,
      kyotoshogi = perfs.kyotoshogi.toRating,
      checkshogi = perfs.checkshogi.toRating,
      ultraBullet = perfs.ultraBullet.toRating,
      bullet = perfs.bullet.toRating,
      blitz = perfs.blitz.toRating,
      rapid = perfs.rapid.toRating,
      classical = perfs.classical.toRating,
      correspondence = perfs.correspondence.toRating
    )

  private def resultOf(game: Game): Glicko.Result =
    game.winnerColor match {
      case Some(shogi.Sente) => Glicko.Result.Win
      case Some(shogi.Gote)  => Glicko.Result.Loss
      case None              => Glicko.Result.Draw
    }

  private def updateRatings(sente: Rating, gote: Rating, result: Glicko.Result): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(sente, gote)
      case Glicko.Result.Win  => results.addResult(sente, gote)
      case Glicko.Result.Loss => results.addResult(gote, sente)
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
        val isStd            = game.variant.standard
        val isHumanVsMachine = player.noBot && opponent.isBot
        def addRatingIf(cond: Boolean, perf: Perf, rating: Rating) =
          if (cond) {
            val p = perf.addOrReset(_.round.error.glicko, s"game ${game.id}")(rating, game.movedAt)
            if (isHumanVsMachine) p averageGlicko perf // halve rating diffs for human
            else p
          } else perf
        val perfs1 = perfs.copy(
          minishogi = addRatingIf(game.variant.minishogi, perfs.minishogi, ratings.minishogi),
          chushogi = addRatingIf(game.variant.chushogi, perfs.chushogi, ratings.chushogi),
          annanshogi = addRatingIf(game.variant.annanshogi, perfs.annanshogi, ratings.annanshogi),
          kyotoshogi = addRatingIf(game.variant.kyotoshogi, perfs.kyotoshogi, ratings.kyotoshogi),
          checkshogi = addRatingIf(game.variant.checkshogi, perfs.checkshogi, ratings.checkshogi),
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
          minishogi = r(PT.Minishogi, perfs.minishogi, perfs1.minishogi),
          chushogi = r(PT.Chushogi, perfs.chushogi, perfs1.chushogi),
          annanshogi = r(PT.Annanshogi, perfs.annanshogi, perfs1.annanshogi),
          kyotoshogi = r(PT.Kyotoshogi, perfs.kyotoshogi, perfs1.kyotoshogi),
          checkshogi = r(PT.Checkshogi, perfs.checkshogi, perfs1.checkshogi),
          bullet = r(PT.Bullet, perfs.bullet, perfs1.bullet),
          blitz = r(PT.Blitz, perfs.blitz, perfs1.blitz),
          rapid = r(PT.Rapid, perfs.rapid, perfs1.rapid),
          classical = r(PT.Classical, perfs.classical, perfs1.classical),
          correspondence = r(PT.Correspondence, perfs.correspondence, perfs1.correspondence)
        )
        if (isStd) perfs2.updateStandard else perfs2
    }
}
