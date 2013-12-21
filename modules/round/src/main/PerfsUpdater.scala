package lila.round

import org.goochjs.glicko2._
import org.joda.time.DateTime
import play.api.Logger

import lila.game.{ GameRepo, Game }
import lila.user.{ UserRepo, HistoryRepo, HistoryEntry, User, Perf, Perfs, Glicko }

object PerfsUpdater {

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d

  val system = new RatingCalculator(VOLATILITY, TAU)

  def save(game: Game): Funit =
    (game.finished && game.turns >= 2) ?? {
      UserRepo.pair(game.whitePlayer.userId, game.blackPlayer.userId) flatMap { users ⇒
        (users match {
          case (Some(white), Some(black)) if game.rated ⇒ {
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
            val perfsW = mkPerfs(ratingsW)
            val perfsB = mkPerfs(ratingsB)
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
                      (UserRepo.setPerfs(white, perfsW, proW) zip UserRepo.setPerfs(black, perfsB, proB))
                  }
                }
          }
          case _ ⇒ funit
        }) >>
          (users._1 ?? { incNbGames(game, _) }) zip
          (users._2 ?? { incNbGames(game, _) }) void
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

  private def incNbGames(game: Game, user: User): Funit =
    UserRepo.incNbGames(user.id, game.rated, game.hasAi,
      result = game.nonAi option (game.winnerUserId match {
        case None          ⇒ 0
        case Some(user.id) ⇒ 1
        case _             ⇒ -1
      })
    )

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

  private implicit def mkPerf(rating: Rating): Perf = Perf(
    Glicko(rating.getRating, rating.getRatingDeviation, rating.getVolatility),
    nb = rating.getNumberOfResults
  )

  def mkPerfs(ratings: Ratings): Perfs = {
    Perfs(
      global = ratings.global,
      standard = ratings.standard,
      chess960 = ratings.chess960,
      bullet = ratings.bullet,
      blitz = ratings.blitz,
      slow = ratings.slow,
      white = ratings.white,
      black = ratings.black)
  }

  private def logger = play.api.Logger("PerfsUpdater")
}
