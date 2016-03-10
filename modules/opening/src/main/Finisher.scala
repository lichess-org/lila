package lila.opening

import org.goochjs.glicko2._
import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONDouble }

import lila.db.Types.Coll
import lila.rating.{ Glicko, Perf }
import lila.user.{ User, UserRepo }

private[opening] final class Finisher(
    api: OpeningApi,
    openingColl: Coll) {

  def apply(opening: Opening, user: User, win: Boolean): Fu[(Attempt, Option[Boolean])] = {
    api.attempt.find(opening.id, user.id) flatMap {
      case Some(a) => fuccess(a -> win.some)
      case None =>
        val userRating = user.perfs.opening.toRating
        val openingRating = opening.perf.toRating
        updateRatings(userRating, openingRating, win.fold(Glicko.Result.Win, Glicko.Result.Loss))
        val date = DateTime.now
        val userPerf = user.perfs.opening.addOrReset(_.opening.crazyGlicko, s"opening ${opening.id} user")(userRating, date)
        val openingPerf = opening.perf.addOrReset(_.opening.crazyGlicko, s"opening ${opening.id}")(openingRating, date)
        val a = new Attempt(
          id = Attempt.makeId(opening.id, user.id),
          openingId = opening.id,
          userId = user.id,
          date = DateTime.now,
          win = win,
          openingRating = opening.perf.intRating,
          openingRatingDiff = openingPerf.intRating - opening.perf.intRating,
          userRating = user.perfs.opening.intRating,
          userRatingDiff = userPerf.intRating - user.perfs.opening.intRating)
        ((api.attempt add a) >> {
          openingColl.update(
            BSONDocument("_id" -> opening.id),
            BSONDocument("$inc" -> BSONDocument(
              Opening.BSONFields.attempts -> BSONInteger(1),
              Opening.BSONFields.wins -> BSONInteger(win ? 1 | 0)
            )) ++ BSONDocument("$set" -> BSONDocument(
              Opening.BSONFields.perf -> Perf.perfBSONHandler.write(openingPerf)
            ))) zip UserRepo.setPerf(user.id, "opening", userPerf)
        }) recover lila.db.recoverDuplicateKey(_ => ()) inject (a -> none)
    }
  }

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  private def mkRating(perf: Perf) = new Rating(
    math.max(1000, perf.glicko.rating),
    perf.glicko.deviation,
    perf.glicko.volatility, perf.nb)

  private def updateRatings(u1: Rating, u2: Rating, result: Glicko.Result) {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(u1, u2)
      case Glicko.Result.Win  => results.addResult(u1, u2)
      case Glicko.Result.Loss => results.addResult(u2, u1)
    }
    try {
      system.updateRatings(results)
    }
    catch {
      case e: Exception => play.api.Logger("Opening trainer").error(e.getMessage)
    }
  }
}
