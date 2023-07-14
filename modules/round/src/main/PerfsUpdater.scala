package lila.round

import chess.{ ByColor, Color, Speed }

import lila.rating.glicko2
import lila.game.{ Game, GameRepo, RatingDiffs }
import lila.history.HistoryApi
import lila.rating.{ Glicko, Perf, PerfType as PT, RatingFactors, RatingRegulator }
import lila.user.{ UserPerfs, RankingApi, User, UserApi }

final class PerfsUpdater(
    gameRepo: GameRepo,
    userApi: UserApi,
    historyApi: HistoryApi,
    rankingApi: RankingApi,
    botFarming: BotFarming,
    ratingFactors: () => RatingFactors
)(using Executor):

  // returns rating diffs
  def save(game: Game, white: User.WithPerfs, black: User.WithPerfs): Fu[Option[RatingDiffs]] =
    botFarming(game) flatMap {
      if _ then fuccess(none)
      else
        game.ratingPerfType.so: mainPerf =>
          (game.rated && game.finished && game.accountable && !white.lame && !black.lame).so:
            val ratingsW = mkRatings(white.perfs)
            val ratingsB = mkRatings(black.perfs)
            game.ratingVariant match
              case chess.variant.Chess960 =>
                updateRatings(ratingsW.chess960, ratingsB.chess960, game)
              case chess.variant.KingOfTheHill =>
                updateRatings(ratingsW.kingOfTheHill, ratingsB.kingOfTheHill, game)
              case chess.variant.ThreeCheck =>
                updateRatings(ratingsW.threeCheck, ratingsB.threeCheck, game)
              case chess.variant.Antichess =>
                updateRatings(ratingsW.antichess, ratingsB.antichess, game)
              case chess.variant.Atomic =>
                updateRatings(ratingsW.atomic, ratingsB.atomic, game)
              case chess.variant.Horde =>
                updateRatings(ratingsW.horde, ratingsB.horde, game)
              case chess.variant.RacingKings =>
                updateRatings(ratingsW.racingKings, ratingsB.racingKings, game)
              case chess.variant.Crazyhouse =>
                updateRatings(ratingsW.crazyhouse, ratingsB.crazyhouse, game)
              case chess.variant.Standard =>
                game.speed match
                  case Speed.Bullet =>
                    updateRatings(ratingsW.bullet, ratingsB.bullet, game)
                  case Speed.Blitz =>
                    updateRatings(ratingsW.blitz, ratingsB.blitz, game)
                  case Speed.Rapid =>
                    updateRatings(ratingsW.rapid, ratingsB.rapid, game)
                  case Speed.Classical =>
                    updateRatings(ratingsW.classical, ratingsB.classical, game)
                  case Speed.Correspondence =>
                    updateRatings(ratingsW.correspondence, ratingsB.correspondence, game)
                  case Speed.UltraBullet =>
                    updateRatings(ratingsW.ultraBullet, ratingsB.ultraBullet, game)
              case _ =>
            val perfsW                     = mkPerfs(ratingsW, white -> black, game)
            val perfsB                     = mkPerfs(ratingsB, black -> white, game)
            def ratingOf(perfs: UserPerfs) = perfs(mainPerf).glicko.intRating
            val ratingDiffs = ByColor(
              ratingOf(perfsW) - ratingOf(white.perfs),
              ratingOf(perfsB) - ratingOf(black.perfs)
            ).map(_ into IntRatingDiff)
            gameRepo.setRatingDiffs(game.id, ratingDiffs) zip
              userApi.updatePerfs(ByColor(white.perfs -> perfsW, black.perfs -> perfsB), game.perfType) zip
              historyApi.add(white.user, game, perfsW) zip
              historyApi.add(black.user, game, perfsB) zip
              rankingApi.save(white.user, game.perfType, perfsW) zip
              rankingApi.save(black.user, game.perfType, perfsB) inject ratingDiffs.some
    }

  private case class Ratings(
      chess960: glicko2.Rating,
      kingOfTheHill: glicko2.Rating,
      threeCheck: glicko2.Rating,
      antichess: glicko2.Rating,
      atomic: glicko2.Rating,
      horde: glicko2.Rating,
      racingKings: glicko2.Rating,
      crazyhouse: glicko2.Rating,
      ultraBullet: glicko2.Rating,
      bullet: glicko2.Rating,
      blitz: glicko2.Rating,
      rapid: glicko2.Rating,
      classical: glicko2.Rating,
      correspondence: glicko2.Rating
  )

  private def mkRatings(perfs: UserPerfs) =
    Ratings(
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

  private def updateRatings(white: glicko2.Rating, black: glicko2.Rating, game: Game): Unit =
    val results = glicko2.GameRatingPeriodResults(
      List(
        game.winnerColor match
          case None              => glicko2.GameResult(white, black, true)
          case Some(chess.White) => glicko2.GameResult(white, black, false)
          case Some(chess.Black) => glicko2.GameResult(black, white, false)
      )
    )
    try Glicko.system.updateRatings(results, true)
    catch case e: Exception => logger.error(s"update ratings #${game.id}", e)

  private def mkPerfs(ratings: Ratings, users: PairOf[User.WithPerfs], game: Game): UserPerfs =
    users match
      case (player, opponent) =>
        val perfs            = player.perfs
        val speed            = game.speed
        val isStd            = game.ratingVariant.standard
        val isHumanVsMachine = player.noBot && opponent.isBot
        def addRatingIf(cond: Boolean, perf: Perf, rating: glicko2.Rating) =
          if cond then
            val p = perf.addOrReset(_.round.error.glicko, s"game ${game.id}")(rating, game.movedAt)
            if isHumanVsMachine
            then p.copy(glicko = p.glicko average perf.glicko) // halve rating diffs for human
            else p
          else perf
        val perfs1 = perfs.copy(
          chess960 = addRatingIf(game.ratingVariant.chess960, perfs.chess960, ratings.chess960),
          kingOfTheHill =
            addRatingIf(game.ratingVariant.kingOfTheHill, perfs.kingOfTheHill, ratings.kingOfTheHill),
          threeCheck = addRatingIf(game.ratingVariant.threeCheck, perfs.threeCheck, ratings.threeCheck),
          antichess = addRatingIf(game.ratingVariant.antichess, perfs.antichess, ratings.antichess),
          atomic = addRatingIf(game.ratingVariant.atomic, perfs.atomic, ratings.atomic),
          horde = addRatingIf(game.ratingVariant.horde, perfs.horde, ratings.horde),
          racingKings = addRatingIf(game.ratingVariant.racingKings, perfs.racingKings, ratings.racingKings),
          crazyhouse = addRatingIf(game.ratingVariant.crazyhouse, perfs.crazyhouse, ratings.crazyhouse),
          ultraBullet =
            addRatingIf(isStd && speed == Speed.UltraBullet, perfs.ultraBullet, ratings.ultraBullet),
          bullet = addRatingIf(isStd && speed == Speed.Bullet, perfs.bullet, ratings.bullet),
          blitz = addRatingIf(isStd && speed == Speed.Blitz, perfs.blitz, ratings.blitz),
          rapid = addRatingIf(isStd && speed == Speed.Rapid, perfs.rapid, ratings.rapid),
          classical = addRatingIf(isStd && speed == Speed.Classical, perfs.classical, ratings.classical),
          correspondence =
            addRatingIf(isStd && speed == Speed.Correspondence, perfs.correspondence, ratings.correspondence)
        )
        val r = RatingRegulator(ratingFactors())
        val perfs2 = perfs1.copy(
          chess960 = r(PT.Chess960, perfs.chess960, perfs1.chess960),
          kingOfTheHill = r(PT.KingOfTheHill, perfs.kingOfTheHill, perfs1.kingOfTheHill),
          threeCheck = r(PT.ThreeCheck, perfs.threeCheck, perfs1.threeCheck),
          antichess = r(PT.Antichess, perfs.antichess, perfs1.antichess),
          atomic = r(PT.Atomic, perfs.atomic, perfs1.atomic),
          horde = r(PT.Horde, perfs.horde, perfs1.horde),
          racingKings = r(PT.RacingKings, perfs.racingKings, perfs1.racingKings),
          crazyhouse = r(PT.Crazyhouse, perfs.crazyhouse, perfs1.crazyhouse),
          bullet = r(PT.Bullet, perfs.bullet, perfs1.bullet),
          blitz = r(PT.Blitz, perfs.blitz, perfs1.blitz),
          rapid = r(PT.Rapid, perfs.rapid, perfs1.rapid),
          classical = r(PT.Classical, perfs.classical, perfs1.classical),
          correspondence = r(PT.Correspondence, perfs.correspondence, perfs1.correspondence)
        )
        if isStd then perfs2.updateStandard else perfs2
